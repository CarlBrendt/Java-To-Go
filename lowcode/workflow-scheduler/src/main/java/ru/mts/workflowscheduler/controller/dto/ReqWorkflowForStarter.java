package ru.mts.workflowscheduler.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import ru.mts.workflowscheduler.engine.ReqStartWorkflow;


@AllArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqWorkflowForStarter {
  ReqStartWorkflow startWorkflow;
  WorkerIdentity workerIdentity;
}
