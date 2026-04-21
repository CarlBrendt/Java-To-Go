package ru.mts.ip.workflow.engine.controller.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;
import ru.mts.ip.workflow.engine.controller.dto.ReqWorkflowExpressionForValidate.ReqActivity;

@Data
@JsonInclude(Include.NON_NULL)
public class ReqWorkflowExpression {
  private String start;
  private List<ReqActivity> activities;
}
