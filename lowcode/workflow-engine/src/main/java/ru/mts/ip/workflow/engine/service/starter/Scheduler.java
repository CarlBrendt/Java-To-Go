package ru.mts.ip.workflow.engine.service.starter;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Scheduler {

  private final StarterTaskService starterTaskService;
  private final StarterService starterService;

  @Scheduled(fixedRate = 10_000) 
  public void processStartWorkflow() {
    starterTaskService.processStartWorkflow();
  }


  @Scheduled(fixedRate = 60_000)
  public void deleteOldTasks() {
    starterTaskService.deleteOldTasks();
  }


  @Scheduled(fixedRate = 60_000)
  public void disableExpiredStarters() {
    starterService.disableExpiredStarters();
  }
}
