package ru.mts.ip.workflow.engine.json;

import java.util.Optional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.mts.ip.workflow.engine.service.blobstorage.BlobSaveOptions;
import ru.mts.ip.workflow.engine.service.blobstorage.BlobRef;

public interface JsonValuesSizeOptimizer {
  ObjectNode optimizeObjectNodeValues(ObjectNode json, BlobSaveOptions blobSaveOptions);
  Optional<BlobRef> optimizeValue(JsonNode value, BlobSaveOptions blobSaveOptions);
  JsonNode resolvePlaceholders(JsonNode node);
}
