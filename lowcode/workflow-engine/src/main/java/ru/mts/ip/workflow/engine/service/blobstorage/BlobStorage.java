package ru.mts.ip.workflow.engine.service.blobstorage;

import java.io.InputStream;
import java.util.Optional;
import org.springframework.core.io.Resource;
import com.fasterxml.jackson.databind.JsonNode;

public interface BlobStorage {
  BlobRef save(JsonNode json, BlobSaveOptions blobSaveOptions);
  JsonNode resolvePlaceholders(JsonNode json);
  Optional<InputStream> find(BlobRef ref);
  Optional<JsonNode> findJson(BlobRef ref);
  BlobRef saveFile(Resource content, BlobSaveOptions blobSaveOptions);
}
