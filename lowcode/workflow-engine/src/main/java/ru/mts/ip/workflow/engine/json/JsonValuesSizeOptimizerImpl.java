package ru.mts.ip.workflow.engine.json;

import java.util.HashMap;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import ru.mts.ip.workflow.engine.EngineConfigurationProperties;
import ru.mts.ip.workflow.engine.service.blobstorage.BlobSaveOptions;
import ru.mts.ip.workflow.engine.service.blobstorage.BlobRef;
import ru.mts.ip.workflow.engine.service.blobstorage.BlobStorage;

@Service
@RequiredArgsConstructor
public class JsonValuesSizeOptimizerImpl implements JsonValuesSizeOptimizer{

  
  private final BlobStorage warehouse;
  private final EngineConfigurationProperties appProps;
  
  @Override
  public ObjectNode optimizeObjectNodeValues(ObjectNode objectNode, BlobSaveOptions blobSaveOptions) {
    var toReplace = new HashMap<String, BlobRef>();
    var fieldNames = objectNode.fieldNames();
    while(fieldNames.hasNext()) {
      var fieldName = fieldNames.next();
      var value = objectNode.get(fieldName);
      optimizeValue(value, blobSaveOptions).ifPresent(ref -> toReplace.put(fieldName, ref));
    }
    toReplace.forEach((k,v) -> {
      objectNode.set(k, new TextNode(v.asLowCodeDecorateVariableRef()));
    });
    return objectNode;
  }

  @Override
  public Optional<BlobRef> optimizeValue(JsonNode value, BlobSaveOptions blobSaveOptions) {
    if(appProps.getMaxVariableSizeBytes() < determineNodeSize(value)) {
      return Optional.of(warehouse.save(value, blobSaveOptions));
    } else {
      return Optional.empty();
    }
  }

  @Override
  public JsonNode resolvePlaceholders(JsonNode node) {
    return warehouse.resolvePlaceholders(node);
  }

  private int determineNodeSize(JsonNode node) {
    return node.toString().getBytes().length;
  }

}
