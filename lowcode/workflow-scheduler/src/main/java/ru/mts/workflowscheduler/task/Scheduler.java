package ru.mts.workflowscheduler.task;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.mts.workflowscheduler.service.SchedulerWorkerLocalManager;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class Scheduler {

  private final SchedulerWorkerLocalManager workers;

  @Scheduled(fixedRate = 15_000)
  public void tryInitialize() {
    workers.tryInitialize();
  }

  @Scheduled(fixedRate = 120_000)
  public void doHeartbeat() {
    workers.doHeartbeat();
  }

  @Scheduled(fixedRate = 60_000)
  public void stopStolenLocalWorkers() {
    workers.stopStolenLocalWorkers();
  }

  @Scheduled(fixedRate = 600_000)
  public void logLocalState() {
    workers.logLocalState();
  }

  @Scheduled(fixedRate = 15_000)
  public void startProcess(){
    workers.startProcess();
  }
}
