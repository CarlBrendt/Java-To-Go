package ru.mts.ip.workflow.engine.dto;

import java.util.UUID;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class StarterErrorContext {
  private UUID id;
  private String type;
  private String name;
}
