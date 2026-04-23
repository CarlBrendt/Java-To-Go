package ru.mts.workflowscheduler.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mts.workflowscheduler.controller.dto.Starter;
import ru.mts.workflowscheduler.controller.dto.Worker;
import ru.mts.workflowscheduler.engine.WorkflowEngine;
import ru.mts.workflowscheduler.utility.DateHelper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Disabled
@ExtendWith(MockitoExtension.class)
class SchedulerExecutorTest {

  @Mock
  private WorkflowEngine workflowEngine;
  @InjectMocks
  private SchedulerExecutor schedulerExecutor;
  private final Clock clock = Clock.fixed(Instant.parse("2025-01-01T13:00:00Z"),  ZoneId.of("UTC"));
  private final UUID workerId = UUID.fromString("580c939b-ce09-4413-8d33-e650a2cb1ee3");
  @BeforeEach
  void setUp() {
    DateHelper.setClock(clock); // Set clock now
  }

  @AfterEach
  void afterEach() {
    DateHelper.resetClock();
  }

  @Test
  void execute_when_overdue_before_now() {
    OffsetDateTime fakeNow = OffsetDateTime.now(clock);
    var overdueBeforeNow = fakeNow.minusMinutes(1);
    UUID workerId = UUID.fromString("580c939b-ce09-4413-8d33-e650a2cb1ee3");
    var worker = createWorkerWIthSimpleDuration(workerId);
    worker.setOverdueTime(overdueBeforeNow);
    schedulerExecutor.execute(workerId);
    verify(workflowEngine, times(1)).startFlow(any(),any());
  }

  @Test
  void not_execute_when_overdue_after_now() {
    DateHelper.setClock(clock); // Set clock now
    OffsetDateTime fakeNow = OffsetDateTime.now(clock);
    var overdueAfterNow = fakeNow.plusMinutes(1);
    UUID workerId = UUID.fromString("580c939b-ce09-4413-8d33-e650a2cb1ee3");
    var worker = createWorkerWIthSimpleDuration(workerId);
    worker.setOverdueTime(overdueAfterNow);
    schedulerExecutor.execute(workerId);
    verify(workflowEngine, times(0)).startFlow(any(),any());
  }

  @Test
  void execute_when_now_between_start_and_end() {
    UUID workerId = UUID.fromString("580c939b-ce09-4413-8d33-e650a2cb1ee3");
    var worker = createWorkerWIthSimpleDuration(workerId);
    schedulerExecutor.execute(workerId);
    verify(workflowEngine, times(1)).startFlow(any(),any());
  }

  @Test
  void not_execute_when_now_not_between_start_and_end() {
    OffsetDateTime fakeNow = OffsetDateTime.now(clock);
    var worker = createWorkerWIthSimpleDuration(workerId);
    var starter = worker.getStarter();
    schedulerExecutor.execute(workerId);
    verify(workflowEngine, times(0)).startFlow(any(),any());
  }

  @Test
  void save_error_when_exception() {
    OffsetDateTime fakeNow = OffsetDateTime.now(clock);
    var worker = createWorkerWIthSimpleDuration(workerId);
    worker.setRetryCount(2);
    doThrow(new ExpectedTestException()).when(workflowEngine).startFlow(any(),any());
    schedulerExecutor.execute(workerId);
  }

  private Worker createWorkerWIthSimpleDuration(UUID workerId) {
    Worker worker = new Worker();
    worker.setId(workerId);
    Starter starter = new Starter();
    Starter.SchedulerStarterDetails details = new Starter.SchedulerStarterDetails();
    details.setType(Const.SchedulerType.SIMPLE);
    details.setSimple(new Starter.SimpleDuration(Duration.ofMinutes(1)));
    worker.setStarter(starter);
    return worker;
  }

  static class ExpectedTestException extends RuntimeException {}
}
