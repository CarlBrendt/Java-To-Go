package ru.mts.workflowscheduler.engine;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

import java.util.UUID;

@Data
@JsonInclude(Include.NON_NULL)
public class ReqRef {
  private UUID id;
  private String name;
  private String tenantId;
}
