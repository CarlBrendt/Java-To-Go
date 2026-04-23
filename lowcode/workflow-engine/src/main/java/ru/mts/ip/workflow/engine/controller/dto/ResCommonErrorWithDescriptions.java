package ru.mts.ip.workflow.engine.controller.dto;

import java.time.OffsetDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;
import lombok.Setter;
import ru.mts.ip.workflow.engine.controller.dto.ResWorkflowDefinitionErrorDescription.ResErrorDescription;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class ResCommonErrorWithDescriptions {

  private OffsetDateTime timestamp;
  private String path;
  private String error;
  private String message;
  private List<ResErrorDescription> errorDescriptions;
  
}
