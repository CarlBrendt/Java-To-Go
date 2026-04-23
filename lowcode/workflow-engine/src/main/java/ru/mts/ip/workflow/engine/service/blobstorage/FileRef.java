package ru.mts.ip.workflow.engine.service.blobstorage;

import java.util.Optional;
import java.util.UUID;
import lombok.Data;

@Data
public class FileRef {
  
  private UUID id;
  
  public FileRef(UUID id) {
    this.id = id;
  }
  
  public String asLowCodeDecorateVariableRef() {
    return "___file_ref{%s}".formatted(id);
  }

  public static Optional<FileRef> fromLowCodeDecorateVariableRef(String text) {
    if(text != null && text.length() == 49 && text.startsWith("___file_ref")) {
      try {
        var id = UUID.fromString(text.substring(13, text.length() - 1));
        return Optional.of(new FileRef(id));
      } catch (IllegalArgumentException  nope) {
        throw new IllegalArgumentException(nope);
      }
    }
    return Optional.empty();
  }
  
}
