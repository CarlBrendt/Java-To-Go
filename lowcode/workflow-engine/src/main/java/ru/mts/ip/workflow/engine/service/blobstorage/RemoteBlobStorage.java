package ru.mts.ip.workflow.engine.service.blobstorage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Optional;

import lombok.SneakyThrows;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.micrometer.core.instrument.util.IOUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.mts.ip.workflow.engine.utility.DateHelper;

import static ru.mts.ip.workflow.engine.service.blobstorage.BlobRef.TYPE_BINARY;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class RemoteBlobStorage implements BlobStorage{
  
  private final BlobStorageServiceClient blobServiceClient;
  private final static ObjectMapper OM = new ObjectMapper();
  
  @Override
  @SneakyThrows
  public BlobRef save(JsonNode json, BlobSaveOptions blobSaveOptions) {
    var bytes =  OM.writeValueAsBytes(json);
    var saved =
        blobServiceClient.save(new ByteArrayResource(bytes),
            blobSaveOptions.getFileName(),
            blobSaveOptions.getRunId(),
            blobSaveOptions.getBusinessKey(),
            blobSaveOptions.getWorkflowDefinitionId(),
            DateHelper.asTextUTC(blobSaveOptions.getExpirationDate(),null));;
    return new BlobRef(saved.getId(), json.getNodeType().ordinal());
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
      return Optional.of(deserializeBlod(JsonNodeType.values()[ref.getType()], textValue));
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

  @Override
  public JsonNode resolvePlaceholders(JsonNode val) {
    if(val.isTextual()) {
      var text = val.asText();
      return BlobRef.fromLowCodeDecorateVariableRef(text)
          .filter(br -> br.getType() != BlobRef.TYPE_BINARY)
          .map(ref -> findJson(ref).orElseThrow(() -> new IllegalStateException("Variable is not found. blobRef: %s".formatted(text))))
          .orElse(val);
    } else if (val.isObject()){
      if(val instanceof ObjectNode on) {
        Iterator<String> it = on.fieldNames();
        while(it.hasNext()) {
          var next = it.next();
          on.set(next, resolvePlaceholders(on.get(next)));
        }
      }
    } else if (val.isArray()){
      if(val instanceof ArrayNode arr) {
        for(int i = 0; i < arr.size(); i++) {
          arr.set(i, resolvePlaceholders(arr.get(i)));
        }
      }
    }
    return val;
    
  }

  public BlobRef saveFile(Resource content, BlobSaveOptions blobSaveOptions) {
    var saved = blobServiceClient.save(content,
        blobSaveOptions.getFileName(),
        blobSaveOptions.getRunId(),
        blobSaveOptions.getBusinessKey(),
        blobSaveOptions.getWorkflowDefinitionId(),
        DateHelper.asTextUTC(blobSaveOptions.getExpirationDate(),null));
    return new BlobRef(saved.getId(), TYPE_BINARY);
  }
  
  
}
