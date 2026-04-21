package ru.mts.ip.workflow.engine.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class ResDefinitionError extends ResCommonErrorWithDescriptions {
  @Schema(requiredMode = RequiredMode.REQUIRED)
  private ResWorkflowDefinition compiledDefinition;
}
