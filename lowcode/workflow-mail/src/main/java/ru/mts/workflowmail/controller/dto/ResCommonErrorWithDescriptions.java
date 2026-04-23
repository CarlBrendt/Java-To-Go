package ru.mts.workflowmail.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;
import lombok.Setter;
import ru.mts.workflowmail.controller.dto.ResRestTemplateErrorDescription.ResErrorDescription;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class ResCommonErrorWithDescriptions {
  private OffsetDateTime timestamp;
  private int status;
  private String error;
  private String path;
  private List<ResErrorDescription> errorDescriptions;
}
