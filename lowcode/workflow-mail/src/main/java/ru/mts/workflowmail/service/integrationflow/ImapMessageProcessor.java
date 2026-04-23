package ru.mts.workflowmail.service.integrationflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.integration.core.GenericHandler;
import org.springframework.messaging.MessageHeaders;
import ru.mts.workflowmail.config.EngineConfigurationProperties;
import ru.mts.workflowmail.controller.dto.Worker;
import ru.mts.workflowmail.engine.WorkflowEngine;
import ru.mts.workflowmail.service.blobstorage.BlobSaveOptions;
import ru.mts.workflowmail.service.dto.Ref;
import ru.mts.workflowmail.service.StarterScriptValidationService;
import ru.mts.workflowmail.service.blobstorage.BlobStorage;
import ru.mts.workflowmail.utility.DateHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class ImapMessageProcessor implements GenericHandler<Object> {
  private final Worker worker;
  private final EngineConfigurationProperties props;
  private final StarterScriptValidationService starterScriptValidationService;
  private final WorkflowEngine workflowEngine;
  private final BlobStorage blobStorage;
  private static final ObjectMapper OM = new ObjectMapper();
  private final ExceptionHolder exceptionHolder;

  @Override
  public Object handle(Object payload, MessageHeaders headers) {

    var starter = worker.getStarter();
    var mailStarterDetail = starter.getMailConsumer();
    var now = DateHelper.now();
    var bk = "%s-%s-%s-%s".formatted(starter.getName(), starter.getTenantId(), worker.getId(),
        DateHelper.asTextISO(now));

    if (payload instanceof Exception ex) {
      exceptionHolder.setException(ex);
    } else
    if (payload instanceof MimeMessage mailMessage) {
      Folder folder = null;
      try {
        MDC.put("starter-id", starter.getId().toString());
        MDC.put("workflow-ref-id", starter.getWorkflowDefinitionToStartId().toString());
        MDC.put("business-key", bk);
        MDC.put("tenant-id", starter.getTenantId());
        String subject = mailMessage.getSubject();
        var senders = Arrays.stream(mailMessage.getFrom())
            .map(a -> ((InternetAddress) a).getAddress())
            .collect(Collectors.joining("; "));

        var  execConfig = props.getWorkflowExecutionConfig();
        var expiration = DateHelper.now().plusSeconds(execConfig.getDefaultExecutionTimeoutSeconds())
            .plusDays(3);

        BlobSaveOptions blobSaveOptions = new BlobSaveOptions()
            .setBusinessKey(bk)
            .setWorkflowDefinitionId(starter.getWorkflowDefinitionToStartId())
            .setExpirationDate(expiration);

        var mailMessageOutput = parseMessage(mailMessage, blobSaveOptions);

        var transformed = starterScriptValidationService.transformMailOutput(mailMessageOutput,
            mailStarterDetail.getOutputTemplate()).orElse(mailMessageOutput);
        var outputValidateErrors = starterScriptValidationService.validateJson(transformed,
            starter.getWorkflowInputValidateSchema());

        if (outputValidateErrors.isEmpty()) {
          workflowEngine.startFlow(new Ref().setId(starter.getWorkflowDefinitionToStartId()),
              bk, transformed, worker.getId());

          log.info("mail received from {} subject is {}; workerId = {}", senders, subject,
              worker.getId());
        } else {
          log.warn(
              "Skipped starter mail message. Reason: incompatible with workflow. Subject: {} MessageOutput:{} Error Details: {}",
              mailMessage.getSubject(), transformed, OM.valueToTree(outputValidateErrors));
        }
        folder = mailMessage.getFolder();
        folder.open(Folder.READ_WRITE);
        mailMessage.setFlag(Flags.Flag.SEEN, true);
        mailMessage.saveChanges();
      } catch (Exception e) {
        log.error("Error handling imap mail message {}", e.getMessage());
        exceptionHolder.setException(e);
        throw new RuntimeException(e);
      } finally {
      // Всегда закрываем folder
      if (folder != null && folder.isOpen()) {
        try {
          folder.close(false);
        } catch (MessagingException ex) {
          log.warn("Error closing folder: {}", ex.getMessage());
        }
      }
      MDC.clear();
    }
    }
    return null;
  }


  private JsonNode parseMessage(MimeMessage mimeMessage, BlobSaveOptions blobSaveOptions) throws MessagingException, IOException {
    String content;
    if (mimeMessage.isMimeType("text/plain") || mimeMessage.isMimeType("text/html")) {
      // Если сообщение содержит только простой текст
      content = (String) mimeMessage.getContent();
    } else if (mimeMessage.isMimeType("multipart/*")) {
      // Если сообщение содержит несколько частей (например, текст и вложения)
      Multipart multipart = (Multipart) mimeMessage.getContent();
      content = getTextFromMultipart(multipart);
    } else {
      // Если тип сообщения неизвестен
      content = "Unsupported message type";
    }
    var from = mimeMessage.getFrom();
    var recipientsTo = mimeMessage.getRecipients(Message.RecipientType.TO);
    var recipientsCc = mimeMessage.getRecipients(Message.RecipientType.CC);
    var sendersNode = OM.createArrayNode();
    var recipientsToNode = OM.createArrayNode();
    var recipientsCcNode = OM.createArrayNode();
    if (from != null && from.length > 0) {
      Arrays.stream(from).map(f -> ((InternetAddress) f).getAddress()).forEach(sendersNode::add);
    }
    if (recipientsTo != null && recipientsTo.length > 0) {
      Arrays.stream(recipientsTo)
          .map(f -> ((InternetAddress) f).getAddress())
          .forEach(recipientsToNode::add);
    }
    if (recipientsCc != null && recipientsCc.length > 0) {
      Arrays.stream(recipientsCc)
          .map(f -> ((InternetAddress) f).getAddress())
          .forEach(recipientsCcNode::add);
    }


    ObjectNode obj = OM.createObjectNode();
    obj.set("content", asNode(content, blobSaveOptions));
    obj.set("contentType", new TextNode(mimeMessage.getContentType()));
    obj.set("sentDate", new TextNode(DateHelper.asTextISO(mimeMessage.getSentDate())));
    obj.set("receiveDate", new TextNode(DateHelper.asTextISO(mimeMessage.getReceivedDate())));
    obj.set("senders", sendersNode);
    obj.set("recipients", recipientsToNode);
    obj.set("recipientsCopy", recipientsCcNode);
    obj.set("subject", new TextNode(mimeMessage.getSubject()));
    return obj;
  }

  private JsonNode asNode(byte[] bytes, BlobSaveOptions blobSaveOptions) {
    if (bytes.length > props.getMaxVariableSizeBytes()) {
      return new TextNode(blobStorage.save(new ByteArrayResource(bytes), blobSaveOptions)
          .setNodeType(JsonNodeType.OBJECT)
          .asLowCodeDecorateVariableRef());
    } else {
      try {
        return OM.readTree(bytes);
      } catch (IOException ex) {
        return new TextNode(new String(bytes, StandardCharsets.UTF_8));
      }
    }
  }

  private JsonNode asNode(String text, BlobSaveOptions blobSaveOptions) {
    return text == null ? OM.nullNode() : asNode(text.getBytes(StandardCharsets.UTF_8), blobSaveOptions);
  }

  private String getTextFromMultipart(Multipart multipart) throws MessagingException, IOException {
    StringBuilder text = new StringBuilder();
    for (int i = 0; i < multipart.getCount(); i++) {
      BodyPart bodyPart = multipart.getBodyPart(i);
      var content = bodyPart.getContent();
      if (bodyPart.isMimeType("text/plain")) {
        // Если часть содержит простой текст
        text.append(content);
      } else if (bodyPart.isMimeType("text/html")) {
        // Если часть содержит HTML, можно пропустить или обработать отдельно
        text.append(content); // или преобразовать HTML в текст
      } else if (content instanceof Multipart multipartContent) {
        // Если часть сама является Multipart (вложенные части)
        text.append(getTextFromMultipart(multipartContent));
      }
    }
    return text.toString();
  }
}
