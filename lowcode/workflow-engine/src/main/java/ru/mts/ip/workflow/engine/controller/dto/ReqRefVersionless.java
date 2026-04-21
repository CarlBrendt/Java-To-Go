package ru.mts.ip.workflow.engine.controller.dto;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class ReqRefVersionless {

  private UUID id;
  private String name;
  private String tenantId;

}