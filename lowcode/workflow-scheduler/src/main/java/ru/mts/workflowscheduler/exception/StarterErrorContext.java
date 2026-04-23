package ru.mts.workflowscheduler.exception;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@Accessors(chain = true)
public class StarterErrorContext {
  private UUID id;
  private String type;
  private String name;
}
