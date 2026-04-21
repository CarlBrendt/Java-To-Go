package ru.mts.ip.workflow.engine.controller.dto.starter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import ru.mts.ip.workflow.engine.controller.dto.ReqStartWorkflow;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqWorkflowForStarter {
  ReqStartWorkflow startWorkflow;
  WorkerIdentity workerIdentity;
}
