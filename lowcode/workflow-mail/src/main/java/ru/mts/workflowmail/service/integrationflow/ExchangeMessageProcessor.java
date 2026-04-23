package ru.mts.workflowmail.service.integrationflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import microsoft.exchange.webservices.data.core.enumeration.service.ConflictResolutionMode;
import microsoft.exchange.webservices.data.core.exception.service.local.ServiceLocalException;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.core.service.item.Item;
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

@Slf4j
@RequiredArgsConstructor
public class ExchangeMessageProcessor implements GenericHandler<Item> {
  private final Worker worker;
  private final EngineConfigurationProperties props;
  private final StarterScriptValidationService starterScriptValidationService;
  private final WorkflowEngine workflowEngine;
  private final BlobStorage blobStorage;
  private static final ObjectMapper OM = new ObjectMapper();
  private final ExceptionHolder exceptionHolder;

  @Override
  public Object handle(Item payload, MessageHeaders headers) {

    var starter = worker.getStarter();
    var mailConsumer = starter.getMailConsumer();
    var now = DateHelper.now();
    var bk = "%s-%s-%s-%s".formatted(starter.getName(), starter.getTenantId(), worker.getId(),
        DateHelper.asTextISO(now));

    if (payload instanceof EmailMessage mailMessage) {
      try {
        MDC.put("starter-id", starter.getId().toString());
        MDC.put("workflow-ref-id", starter.getWorkflowDefinitionToStartId().toString());
        MDC.put("business-key", bk);
        MDC.put("tenant-id", starter.getTenantId());

        String subject = mailMessage.getSubject();
        var senders = mailMessage.getFrom();

        var  execConfig = props.getWorkflowExecutionConfig();
        var expiration = DateHelper.now().plusSeconds(execConfig.getDefaultExecutionTimeoutSeconds())
            .plusDays(3);

        BlobSaveOptions blobSaveOptions = new BlobSaveOptions()
            .setBusinessKey(bk)
            .setWorkflowDefinitionId(starter.getWorkflowDefinitionToStartId())
            .setExpirationDate(expiration);
        var mailMessageOutput = parseMessage(mailMessage, blobSaveOptions);

        var transformed = starterScriptValidationService.transformMailOutput(mailMessageOutput,
            mailConsumer.getOutputTemplate()).orElse(mailMessageOutput);
        var outputValidateErrors = starterScriptValidationService.validateJson(transformed,
            starter.getWorkflowInputValidateSchema());

        if (outputValidateErrors.isEmpty()) {
          workflowEngine.startFlow(new Ref().setId(starter.getWorkflowDefinitionToStartId()), bk,
              transformed, worker.getId());

          log.info("mail received from {} subject is {}; workerId = {}", senders, subject,
              worker.getId());
        } else {
          log.warn(
              "Skipped starter mail message. Reason: incompatible with workflow. Subject: {} MessageOutput:{} Error Details: {}",
              mailMessage.getSubject(), transformed, OM.valueToTree(outputValidateErrors));
        }
        mailMessage.setIsRead(true);
        mailMessage.update(ConflictResolutionMode.AutoResolve);
      } catch (Exception e) {
        log.error("Error handling exchange mail message {}", e.getMessage());
        exceptionHolder.setException(e);
        throw new RuntimeException(e);
      } finally {
        MDC.clear();
      }
    }
    return null;
  }


  private JsonNode parseMessage(EmailMessage emailMessage, BlobSaveOptions blobSaveOptions) throws ServiceLocalException {
    String content;
    content = emailMessage.getBody().toString();

    var from = emailMessage.getFrom();
    var recipientsTo = emailMessage.getToRecipients();
    var recipientsCc = emailMessage.getCcRecipients();
    var sendersNode = OM.createArrayNode();
    var recipientsToNode = OM.createArrayNode();
    var recipientsCcNode = OM.createArrayNode();
    if (from != null) {
      sendersNode.add(from.getAddress());
    }
    if (recipientsTo != null && recipientsTo.getCount() > 0) {
      recipientsTo.forEach(recipient -> recipientsToNode.add(recipient.getAddress()));
    }
    if (recipientsCc != null && recipientsCc.getCount() > 0) {
      recipientsCc.forEach(recipient -> recipientsCcNode.add(recipient.getAddress()));
    }

    ObjectNode obj = OM.createObjectNode();
    obj.set("content", asNode(content, blobSaveOptions));
    obj.set("contentType", new TextNode(emailMessage.getBody().getBodyType().toString()));
    obj.set("sentDate", new TextNode(DateHelper.asTextISO(emailMessage.getDateTimeSent())));
    obj.set("receiveDate", new TextNode(DateHelper.asTextISO(emailMessage.getDateTimeReceived())));
    obj.set("senders", sendersNode);
    obj.set("recipients", recipientsToNode);
    obj.set("recipientsCopy", recipientsCcNode);
    obj.set("subject", new TextNode(emailMessage.getSubject()));
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
    return text == null ? OM.nullNode() : asNode(text.getBytes(StandardCharsets.UTF_8), blobSaveOptions );
  }
}
