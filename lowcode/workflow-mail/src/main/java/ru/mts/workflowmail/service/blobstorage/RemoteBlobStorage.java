package ru.mts.workflowmail.service.blobstorage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.TextNode;
import io.micrometer.core.instrument.util.IOUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import ru.mts.workflowmail.utility.DateHelper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class RemoteBlobStorage implements BlobStorage{

  private final BlobStorageServiceClient blobServiceClient;
  private final static ObjectMapper OM = new ObjectMapper();

  @Override
  public BlobRef save(Resource content, BlobSaveOptions options) {
    var saved = blobServiceClient.save(content,
        options.getFileName(),
        options.getRunId(),
        options.getBusinessKey(),
        options.getWorkflowDefinitionId(),
        DateHelper.asTextUTC(options.getExpirationDate(),null));
    return new BlobRef().setId(saved.getId());
  }

  @Override
  public Optional<InputStream> find(BlobRef ref) {
    var resp = blobServiceClient.find(ref.getId());
    var body = resp.getBody();
    if(body != null) {
      try {
        return Optional.ofNullable(body.getInputStream());
      } catch (IOException ex) {
        log.error("Error receiving blob content", ex);
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<JsonNode> findJson(BlobRef ref) {
    String textValue = null;
    var is = find(ref).orElse(null);
    if(is != null) {
      try(var toClose = is){
        textValue = IOUtils.toString(toClose, StandardCharsets.UTF_8);
      } catch (IOException ex) {
        log.error("Reading blob error", ex);
        textValue = null;
      }
    }
    if(textValue != null) {
      return Optional.of(deserializeBlod(ref.getNodeType(), textValue));
    }
    return Optional.empty();
  }

  private JsonNode deserializeBlod(JsonNodeType type, String text) {
    if (type == JsonNodeType.STRING) {
      return new TextNode(text);
    } else {
      try {
        return OM.readTree(text);
      } catch (IOException ex) {
        log.error("json parse error, TextNode as result returned", ex);
        return new TextNode(text);
      }
    }
  }

}
