package ru.mts.ip.workflow.engine.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.utility.DateHelper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static ru.mts.ip.workflow.engine.Const.SchedulerType.SIMPLE;
import static ru.mts.ip.workflow.engine.Const.StarterType.SCHEDULER;

class WorkerEntityTest {
  private final Clock clock = Clock.fixed(Instant.parse("2025-01-01T13:00:00Z"), ZoneId.of("UTC"));

  @BeforeEach
  void setUp() {
    DateHelper.setClock(clock); // Set clock now
  }

  @AfterEach
  void afterEach() {
    DateHelper.resetClock();
  }

  @Test
  void success() {
    var entity = new WorkerEntity();
    var details = new SchedulerStarterDetails().setType(SIMPLE).setSimple(new SchedulerStarterDetails.SimpleDuration(Duration.ofMinutes(1)));
    entity.setStarter(new StarterEntity().setScheduler(details).setType(SCHEDULER));
    var res = entity.success();
    OffsetDateTime now = OffsetDateTime.now(clock);
    assertTrue(res.getLockedUntilTime().isAfter(now));
    assertNotNull(res.getRetryCount());
    assertNotNull(res.getOverdueTime());
  }

  @Test
  void error() {
    var worker = new WorkerEntity();
    var starter = new StarterEntity().setType(SCHEDULER)
        .setScheduler(
        new SchedulerStarterDetails().setType(SIMPLE)
        .setSimple(new SchedulerStarterDetails.SimpleDuration(Duration.ofMinutes(1)))
        );
    worker.setStarter(starter);
    worker.setRetryCount(2);

    String expectedMessage = "test error message";
    String stackTrace = "stackTrace";
    var res = worker.error(expectedMessage, stackTrace);
    assertEquals(Const.StarterStatus.ERROR, res.getStatus());
    assertNull(res.getLockedUntilTime());
    assertNotNull(res.getOverdueTime());
    assertEquals(1, res.getRetryCount());
    assertEquals(expectedMessage, res.getErrorMessage());
    assertEquals(stackTrace, res.getErrorStackTrace());
  }

}
