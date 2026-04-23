package ru.mts.ip.workflow.engine.controller.dto;

import java.time.OffsetDateTime;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class ResCommonError {
  private OffsetDateTime timestamp;
  private String path;
  private String error;
  private String message;
}
