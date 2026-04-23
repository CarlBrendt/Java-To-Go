package ru.mts.ip.workflow.engine.service.starter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapper;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapperImpl;
import ru.mts.ip.workflow.engine.controller.dto.starter.ResWorker;
import ru.mts.ip.workflow.engine.entity.StarterEntity;
import ru.mts.ip.workflow.engine.entity.WorkerEntity;
import ru.mts.ip.workflow.engine.repository.StarterWorkerRepository;
import ru.mts.ip.workflow.engine.repository.TransactionHelper;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkerServiceImplTest {

  @Mock
  private StarterWorkerRepository repository;
  @Spy
  private TransactionHelper th = new TransactionHelper();

  @Spy
  private DtoMapper workerMapper = new DtoMapperImpl();

  @Mock
  private StarterService starterService;

  @InjectMocks
  private WorkerServiceImpl workerService;

  @Test
  void getWorker_WhenWorkerExists_ShouldReturnWorkerWithAllFields() {
    // Arrange
    UUID workerId = UUID.randomUUID();
    UUID executorId = UUID.randomUUID();
    UUID starterId = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now();

    // Создаем StarterEntity с заполненными полями
    StarterEntity starterEntity = new StarterEntity();
    starterEntity.setId(starterId);
    starterEntity.setType("SCHEDULER");
    starterEntity.setName("Test Starter");
    starterEntity.setDescription("Test Description");
    starterEntity.setDesiredStatus("ACTIVE");
    starterEntity.setActualStatus("ACTIVE");
    starterEntity.setCreateTime(now.minusHours(1));
    starterEntity.setChangeTime(now.minusMinutes(30));
    starterEntity.setStartDateTime(now);
    starterEntity.setEndDateTime(now.plusHours(1));
    // workflowDefinition и details оставляем null
    starterEntity.setExclusions(null);

    // Создаем WorkerEntity с заполненными полями
    WorkerEntity workerEntity = new WorkerEntity();
    workerEntity.setId(workerId);
    workerEntity.setVersion(1L);
    workerEntity.setCreateTime(now.minusHours(2));
    workerEntity.setChangeTime(now.minusMinutes(15));
    workerEntity.setExecutorId(executorId);
    workerEntity.setStarter(starterEntity);
    workerEntity.setLockedUntilTime(now.plusMinutes(WorkerEntity.RUNNING_TIMEOUT_MINUTES));
    workerEntity.setOverdueTime(now.plusMinutes(WorkerEntity.RUNNING_TIMEOUT_MINUTES + 5));
    workerEntity.setRetryCount(WorkerEntity.INITIAL_RETRY_COUNT);
    workerEntity.setStatus("RUNNING");
    workerEntity.setErrorMessage("Test error message");
    workerEntity.setErrorStackTrace("Test stack trace");

    starterEntity.setWorker(workerEntity);

    // Создаем ожидаемый результат
    ResWorker expectedResWorker = new ResWorker();
    expectedResWorker.setId(workerId);
    expectedResWorker.setCreateTime(now.minusHours(2));
    expectedResWorker.setChangeTime(now.minusMinutes(15));
    expectedResWorker.setExecutorId(executorId);
    //expectedResWorker.setStarter(starterEntity);
    expectedResWorker.setLockedUntilTime(now.plusMinutes(WorkerEntity.RUNNING_TIMEOUT_MINUTES));
    expectedResWorker.setOverdueTime(now.plusMinutes(WorkerEntity.RUNNING_TIMEOUT_MINUTES + 5));
    expectedResWorker.setRetryCount(WorkerEntity.INITIAL_RETRY_COUNT);
    expectedResWorker.setStatus("RUNNING");
    expectedResWorker.setErrorMessage("Test error message");
    expectedResWorker.setErrorStackTrace("Test stack trace");

    ResWorker.ResInnerStarter expectedStarter = new ResWorker.ResInnerStarter();
    expectedStarter.setId(starterId);
    expectedStarter.setType("SCHEDULER");
    expectedStarter.setName("Test Starter");
    expectedStarter.setDescription("Test Description");
    expectedStarter.setDesiredStatus("ACTIVE");
    expectedStarter.setActualStatus("ACTIVE");
    expectedStarter.setCreateTime(now.minusHours(1));
    expectedStarter.setChangeTime(now.minusMinutes(30));

    expectedResWorker.setStarter(expectedStarter);

    when(repository.findById(workerId)).thenReturn(Optional.of(workerEntity));
    // Act
    Optional<ResWorker> result = workerService.getWorker(workerId);

    // Assert
    assertTrue(result.isPresent(), "Worker should be present");
    ResWorker actualResWorker = result.get();

    assertEquals(expectedResWorker, actualResWorker, "Returned worker should match expected");
  }

  @Test
  void getWorker_WhenWorkerDoesNotExist_ShouldReturnEmptyOptional() {
    // Arrange
    UUID nonExistentWorkerId = UUID.randomUUID();

    when(repository.findById(nonExistentWorkerId)).thenReturn(Optional.empty());

    // Act
    Optional<ResWorker> result = workerService.getWorker(nonExistentWorkerId);

    // Assert
    assertFalse(result.isPresent(), "Worker should not be present for non-existent ID");
  }

  @Test
  void getWorker_WhenWorkerHasNullFields_ShouldHandleGracefully() {
    // Arrange
    UUID workerId = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now();

    // Создаем WorkerEntity с некоторыми null полями
    WorkerEntity workerEntity = new WorkerEntity();
    workerEntity.setId(workerId);
    workerEntity.setVersion(1L);
    workerEntity.setCreateTime(now);
    workerEntity.setChangeTime(now);
    // executorId = null
    // starter = null
    workerEntity.setLockedUntilTime(now.plusMinutes(10));
    // overdueTime = null
    workerEntity.setRetryCount(0);
    workerEntity.setStatus("PENDING");
    // errorMessage = null
    // errorStackTrace = null

    ResWorker expectedResWorker = new ResWorker();
    // Заполняем expectedResWorker с соответствующими null значениями

    when(repository.findById(workerId)).thenReturn(Optional.of(workerEntity));

    // Act
    Optional<ResWorker> result = workerService.getWorker(workerId);

    // Assert
    assertTrue(result.isPresent(), "Worker should be present even with null fields");
  }
}
