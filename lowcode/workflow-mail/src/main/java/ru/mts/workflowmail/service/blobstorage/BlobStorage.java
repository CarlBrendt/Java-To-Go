package ru.mts.workflowmail.service.blobstorage;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.Optional;

public interface BlobStorage {

  BlobRef save(Resource content, BlobSaveOptions options);

  Optional<InputStream> find(BlobRef ref);
  Optional<JsonNode> findJson(BlobRef ref);
}
