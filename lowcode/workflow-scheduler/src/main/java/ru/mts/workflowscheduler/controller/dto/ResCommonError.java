package ru.mts.workflowscheduler.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class ResCommonError {
  private OffsetDateTime timestamp;
  private int status;
  private String error;
  private String path;
}
