package ru.mts.workflowscheduler.controller.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.workflowscheduler.controller.dto.ResRestTemplateErrorDescription.ResErrorDescription;

import java.util.List;

@Data
@Accessors(chain = true)
public class ResValidationResult {
  private List<ResErrorDescription> errors;
}
