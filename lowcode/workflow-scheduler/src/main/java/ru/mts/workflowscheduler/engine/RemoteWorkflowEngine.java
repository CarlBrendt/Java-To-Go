package ru.mts.workflowscheduler.engine;

import com.fasterxml.jackson.databind.JsonNode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mts.workflowscheduler.controller.dto.ReqWorkflowForStarter;
import ru.mts.workflowscheduler.controller.dto.Worker;
import ru.mts.workflowscheduler.controller.dto.WorkerError;
import ru.mts.workflowscheduler.controller.dto.WorkerIdentity;
import ru.mts.workflowscheduler.entity.Ref;
import ru.mts.workflowscheduler.mapper.DtoMapper;
import ru.mts.workflowscheduler.service.Const;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RemoteWorkflowEngine implements WorkflowEngine{

  private final WorkflowEngineClient client;
  private final DtoMapper dtoMapper;

  @Override
  public void startFlow(Ref definitionRef, String businessKey) {
    startFlow(definitionRef, businessKey, null, null);
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

  @Override
  public Optional<Worker> getWorkerAndLock(UUID workerId){
    try {
      return Optional.of(client.getWorker(workerId));
    } catch (FeignException.NotFound ignored) {}
    return Optional.empty();
  }


  @Override
  public void startFlow(Ref definitionRef, String bk, UUID workerId) {
    startFlow(definitionRef, bk, null, workerId);
  }

}
