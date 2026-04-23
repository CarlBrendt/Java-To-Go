package ru.mts.workflowscheduler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import ru.mts.workflowscheduler.controller.dto.WorkerError;
import ru.mts.workflowscheduler.engine.WorkflowEngine;
import ru.mts.workflowscheduler.entity.Ref;
import ru.mts.workflowscheduler.utility.DateHelper;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerExecutor {
  private final WorkflowEngine workflowEngine;


  public void execute(UUID workerId) {
    workflowEngine.getWorkerAndLock(workerId).ifPresent(worker -> {
      try {
        var starter = worker.getStarter();
        var now = DateHelper.now();
        var bk = "%s-%s-%s-%s".formatted(starter.getName(), starter.getTenantId(), worker.getId(),
            DateHelper.asTextISO(now));

        MDC.put("starter-id", starter.getId().toString());
        MDC.put("workflow-ref-id", starter.getWorkflowDefinitionToStartId().toString());
        MDC.put("business-key", bk);
        MDC.put("tenant-id", starter.getTenantId());

        var overdueTime = worker.getOverdueTime();
        if ((overdueTime == null || overdueTime.isBefore(now))) {
          workflowEngine.startFlow(new Ref().setId(starter.getWorkflowDefinitionToStartId()), bk, worker.getId());
          log.info("process started with businessKey {}", bk);
        }
      } catch (Exception ex) {
        log.error("starting process failed", ex);
      } finally {
        MDC.clear();
      }

    });
  }


}
