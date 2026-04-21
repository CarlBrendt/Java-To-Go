package ru.mts.ip.workflow.engine.service.blobstorage;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;

@Service("in-memory")
public class InMemoryBlobStorage implements BlobStorage{

  private Map<BlobRef, JsonNode> storage = new ConcurrentHashMap<>();
  private Map<BlobRef, byte[]> fileStorage = new ConcurrentHashMap<>();
  
  @Override
  public BlobRef save(JsonNode json, BlobSaveOptions blobSaveOptions) {
    var ref = new BlobRef(UUID.randomUUID(), json.getNodeType());
    storage.put(ref, json);
    return ref;
  }

  @Override
  public Optional<JsonNode> findJson(BlobRef ref) {
    return Optional.ofNullable(storage.get(ref));
  }

  @Override
  public JsonNode resolvePlaceholders(JsonNode json) {
    return json;
  }

  @Override
  public Optional<InputStream> find(BlobRef ref) {
    return Optional.empty();
  }

  @Override
  @SneakyThrows
  public BlobRef saveFile(Resource content, BlobSaveOptions blobSaveOptions) {
    var ref = new BlobRef(UUID.randomUUID(), BlobRef.TYPE_BINARY);
    fileStorage.put(ref, content.getContentAsByteArray());
    return ref;
  }

}
