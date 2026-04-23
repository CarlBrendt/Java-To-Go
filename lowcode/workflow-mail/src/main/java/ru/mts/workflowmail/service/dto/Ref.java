package ru.mts.workflowmail.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@Accessors(chain = true)
@JsonInclude(Include.NON_NULL)
public class Ref {
  private UUID id;
  private String name;
  private Integer version;
  private String tenantId;
}
