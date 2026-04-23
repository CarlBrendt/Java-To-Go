package ru.mts.workflowmail.engine;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mts.workflowmail.controller.dto.ReqWorkflowForStarter;
import ru.mts.workflowmail.controller.dto.WorkerIdentity;
import ru.mts.workflowmail.service.Const;
import ru.mts.workflowmail.service.dto.Ref;
import ru.mts.workflowmail.mapper.DtoMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RemoteWorkflowEngine implements WorkflowEngine{

  private final WorkflowEngineClient client;
  private final DtoMapper dtoMapper;

  @Override
  public void startFlow(Ref definitionRef, String businessKey, UUID workerId) {
    startFlow(definitionRef, businessKey, null, workerId);
  }

  @Override
  public void startFlow(Ref definitionRef, String businessKey, JsonNode vars, UUID workerId) {

    ReqStartWorkflow reqStartWorkflow = new ReqStartWorkflow()
        .setWorkflowStartConfig(
            new ReqStartWorkflow.ReqWorkflowStartConfig()
              .setBusinessKey(businessKey)
              .setVariables(vars)
        )
        .setWorkflowRef(dtoMapper.toReqRef(definitionRef));

    var workerIdentity = new WorkerIdentity(workerId, Const.getApplicationInstanceId());
    ReqWorkflowForStarter request = new ReqWorkflowForStarter(reqStartWorkflow, workerIdentity);
    client.startWorkflow(request);
  }
}
