package ru.mts.ip.workflow.engine.service.starter;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapper;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqWorkerReplace;
import ru.mts.ip.workflow.engine.controller.dto.starter.ResWorker;
import ru.mts.ip.workflow.engine.controller.dto.starter.WorkerError;
import ru.mts.ip.workflow.engine.controller.dto.starter.WorkerExtend;
import ru.mts.ip.workflow.engine.controller.dto.starter.WorkerIdentity;
import ru.mts.ip.workflow.engine.controller.dto.starter.WorkerLocker;
import ru.mts.ip.workflow.engine.entity.WorkerEntity;
import ru.mts.ip.workflow.engine.json.JsonSerializer;
import ru.mts.ip.workflow.engine.repository.StarterWorkerRepository;
import ru.mts.ip.workflow.engine.repository.TransactionHelper;
import ru.mts.ip.workflow.engine.utility.DateHelper;
import ru.mts.ip.workflow.engine.utility.ErrorHelper;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerServiceImpl implements WorkerService {

  private final StarterWorkerRepository repository;
  private final TransactionHelper th;
  private final DtoMapper mapper;
  private final JsonSerializer jsonSerializer;
  private final StarterService starterService;

  @Override
  @Transactional
  public Optional<ResWorker> getExecutableWorkerAndLock(WorkerLocker locker) {
    try {
      return th.inNewTransaction(
          //сделал сначала поиск id потом повторный запрос чтобы избежать получения всех записей
          //HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory
          () -> repository.findFirstOrderByLockedTime(locker.getStatuses(), locker.getType(), DateHelper.now())
          .flatMap(repository::findById)
          .map(w -> {
            var entity = repository.save(w.lock(locker.getExecutorId()));
            return mapToResWorker(entity);
          }));
    } catch (ObjectOptimisticLockingFailureException ex) {
      return getExecutableWorkerAndLock(locker);
    }
  }

  @Override
  @Transactional
  public void startWorker(WorkerIdentity w) {
    var worker = repository.findByIdAndExecutorId(w.workerId(), w.executorId())
        .map(WorkerEntity::run)
        .orElseThrow(() -> ErrorHelper.workerIsNotFound(w.workerId(),w.executorId()));

    worker.getStarter().setActualStatus(Const.StarterStatus.STARTED);
  }

  @Override
  @Transactional
  public void errorWorker(WorkerError e) {
    var worker = repository.findById(e.workerId())
        .orElseThrow(() -> ErrorHelper.workerIsNotFound(e.workerId()));

    if (worker.getExecutorId() != null && !worker.getExecutorId().equals(e.executorId())) {
       log.warn("Worker error update with wrong executor id: {}, current is {}", e.executorId(), worker.getExecutorId());
    }

    worker.error(e.errorMessage(), e.stackTrace());
    var starter = worker.getStarter();
    starter.setActualStatus(Const.StarterStatus.ERROR);
  }

  @Override
  @Transactional
  public void extendWorker(WorkerExtend extend) {
    repository.updateLockUntilTime(
        DateHelper.now().plusMinutes(WorkerEntity.RUNNING_TIMEOUT_MINUTES), extend.workerIds(),
        extend.executorId());
  }

  @Override
  @Transactional
  public void deleteWorker(UUID id) {
    if (repository.existsById(id)) {
      repository.updateWorkerStatus(id, Const.WorkerStatus.SCHEDULED_TO_DELETE);
    } else {
      throw ErrorHelper.workerIsNotFound(id);
    }
  }

  @Override
  @Transactional
  public void replaceWorker(UUID id, ReqWorkerReplace replaceBy) {
    var toReplace = repository.findById(id).orElseThrow(() -> ErrorHelper.workerIsNotFound(id));
    toReplace.setRetryCount(replaceBy.getRetryCount());
    toReplace.setStatus(replaceBy.getStatus());
    repository.save(toReplace);
  }

  @Override
  public Set<UUID> getWorkerIds(UUID executorId) {
    return repository.findIdsByExecutorId(executorId);
  }

  @Override
  @Transactional
  public Optional<ResWorker> getWorker(UUID id) {
    return repository.findById(id)
        .map(this::mapToResWorker);
  }

  @Override
  @Transactional
  public void successWorker(UUID workerId) {
    var worker = repository.findById(workerId).orElseThrow(()-> ErrorHelper.workerIsNotFound(workerId));
    repository.save(worker.success());
  }

  @Override
  public void checkWorkerExists(WorkerIdentity wi) {
    if (!repository.existsByIdAndExecutorId(wi.workerId(), wi.executorId())) {
      throw ErrorHelper.workerIsNotFound(wi.workerId(), wi.executorId());
    }
  }

  @NotNull
  private ResWorker mapToResWorker(@NotNull WorkerEntity entity) {
    var workerDto = mapper.toWorkerDto(entity);
    var starter = mapper.toStarter(entity.getStarter());
    if (starter != null) {
      starter.setWorker(null);
      starterService.applySecrets(starter);
      workerDto.setStarter(starter);
    }
    return mapper.toResWorker(workerDto);
  }
}
