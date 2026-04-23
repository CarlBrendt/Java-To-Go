package ru.mts.workflowmail.service.blobstorage;

import com.fasterxml.jackson.databind.node.JsonNodeType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Data
@Accessors(chain = true)
@NoArgsConstructor
public class BlobRef {

  private UUID id;
  private JsonNodeType nodeType;
  private final static String LEFT_ANCHOR = "___blob_ref_";

  public BlobRef(UUID id, JsonNodeType nodeType) {
    Objects.requireNonNull(nodeType);
    this.id = id;
    this.nodeType = nodeType;
  }

  public String asLowCodeDecorateVariableRef() {
    return "%s%02d{%s}".formatted(LEFT_ANCHOR, nodeType.ordinal(), id);
  }

  public static Optional<BlobRef> fromLowCodeDecorateVariableRef(String text) {
    if(text != null && text.length() == 52 && text.startsWith(LEFT_ANCHOR)) {
      try {
        var id = UUID.fromString(text.substring(15, text.length() - 1));
        var type = JsonNodeType.values()[Integer.valueOf(text.substring(12, 14))];
        return Optional.of(new BlobRef(id, type));
      } catch (IllegalArgumentException  nope) {
        throw new IllegalArgumentException(nope);
      }
    }
    return Optional.empty();
  }

}
