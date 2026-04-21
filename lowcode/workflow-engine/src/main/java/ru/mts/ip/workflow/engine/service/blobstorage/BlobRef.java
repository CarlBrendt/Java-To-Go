package ru.mts.ip.workflow.engine.service.blobstorage;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import lombok.Data;

@Data
public class BlobRef {
  
  private UUID id;
  private Integer type;
  private final static String LEFT_ANCHOR = "___blob_ref_";
  
  public static final int TYPE_JSON_ARRAY = JsonNodeType.ARRAY.ordinal();
  public static final int TYPE_JSON_BINARY = JsonNodeType.BINARY.ordinal();
  public static final int TYPE_JSON_BOOLEAN = JsonNodeType.BOOLEAN.ordinal();
  public static final int TYPE_JSON_MISSING = JsonNodeType.MISSING.ordinal();
  public static final int TYPE_JSON_NULL = JsonNodeType.NULL.ordinal();
  public static final int TYPE_JSON_NUMBER = JsonNodeType.NUMBER.ordinal();
  public static final int TYPE_JSON_OBJECT = JsonNodeType.OBJECT.ordinal();
  public static final int TYPE_JSON_POJO = JsonNodeType.POJO.ordinal();
  public static final int TYPE_JSON_STRING = JsonNodeType.STRING.ordinal();
  public static final int TYPE_BINARY = 20;

  public BlobRef(UUID id, Integer type) {
    Objects.requireNonNull(type);
    this.id = id;
    this.type = type;
  }

  public BlobRef(UUID randomUUID, JsonNodeType nodeType) {
    this(randomUUID, nodeType.ordinal());
  }

  public String asLowCodeDecorateVariableRef() {
    return "%s%02d{%s}".formatted(LEFT_ANCHOR, type, id);
  }
  
  public static Optional<BlobRef> fromLowCodeDecorateVariableRef(String text) {
    if(text != null && text.length() == 52 && text.startsWith(LEFT_ANCHOR)) {
      try {
        var id = UUID.fromString(text.substring(15, text.length() - 1));
        return Optional.of(new BlobRef(id, Integer.parseInt(text.substring(12, 14))));
      } catch (IllegalArgumentException  nope) {
        throw new IllegalArgumentException(nope);
      }
    }
    return Optional.empty();
  }
  
}
