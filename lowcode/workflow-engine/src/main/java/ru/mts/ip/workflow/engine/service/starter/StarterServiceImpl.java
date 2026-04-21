package ru.mts.ip.workflow.engine.service.starter;

import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.Const.StarterStatus;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapper;
import ru.mts.ip.workflow.engine.controller.dto.ResReplacedStarter;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqStopStarter;
import ru.mts.ip.workflow.engine.controller.dto.starter.patch.ReqStarterPatch;
import ru.mts.ip.workflow.engine.dto.DefinitionDetails;
import ru.mts.ip.workflow.engine.dto.Ref;
import ru.mts.ip.workflow.engine.dto.SapInboundStarter;
import ru.mts.ip.workflow.engine.dto.Starter;
import ru.mts.ip.workflow.engine.entity.StarterEntity;
import ru.mts.ip.workflow.engine.entity.WorkerEntity;
import ru.mts.ip.workflow.engine.exception.ClientError;
import ru.mts.ip.workflow.engine.exception.ClientErrorDescription;
import ru.mts.ip.workflow.engine.exception.ErrorMessagePouch;
import ru.mts.ip.workflow.engine.executor.ExternalProperties;
import ru.mts.ip.workflow.engine.executor.ResolveExternalPropertiesConfig;
import ru.mts.ip.workflow.engine.executor.WorkflowExecutorService;
import ru.mts.ip.workflow.engine.json.JsonSerializer;
import ru.mts.ip.workflow.engine.repository.StarterRepository;
import ru.mts.ip.workflow.engine.repository.StarterWorkerRepository;
import ru.mts.ip.workflow.engine.repository.TransactionHelper;
import ru.mts.ip.workflow.engine.repository.WorkflowDefinitionRepositoryHelper;
import ru.mts.ip.workflow.engine.service.EncryptedDataHandler;
import ru.mts.ip.workflow.engine.service.dto.StarterSearching;
import ru.mts.ip.workflow.engine.service.dto.StarterShortListValue;
import ru.mts.ip.workflow.engine.utility.ErrorHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static ru.mts.ip.workflow.engine.Const.Errors2.WORKFLOW_IS_NOT_FOUND_BY_REF;
import static ru.mts.ip.workflow.engine.Const.StarterStatus.EXPIRED;

@Slf4j
@Service
@RequiredArgsConstructor
public class StarterServiceImpl implements StarterService {

  private final StarterRepository starterRepository;
  private final StarterWorkerRepository workerRepository;
  private final WorkflowDefinitionRepositoryHelper workflowDefinitionRepositoryHelper;
  private final TransactionHelper th;
  private final DtoMapper mapper;
  private final JsonSerializer jsonSerializer;
  private final WorkflowExecutorService executor;
  private final StarterPatchService starterPatchService;

  @Transactional
  public ResReplacedStarter createOrReplaceStarter(@NonNull Starter starter) {

    if (Const.StarterType.REST_CALL.equalsIgnoreCase(starter.getType())){
      log.debug("rest_call is default starter. It can't be created or replaced");
      return new ResReplacedStarter(null,null);
    }
    applySecrets(starter);

    var workflowDefinition =  workflowDefinitionRepositoryHelper.findDeployedDefinition(new Ref().setId(starter.getWorkflowDefinitionToStartId())).orElseThrow();
    StarterEntity entity = mapper.toStarterEntity(starter, workflowDefinition);
    Optional<StarterEntity> oldStarter =
        starterRepository.findByTypeAndNameAndTenantId(starter.getType(), starter.getName(), workflowDefinition.getTenantId());
    ResReplacedStarter.ResShortStarter old = null;
    if (oldStarter.isPresent()) {
      workerRepository.deleteWorkersForStarter(oldStarter.get().getId());
      entity.setId(oldStarter.get().getId());
      entity.setCreateTime(oldStarter.get().getCreateTime());
      old = new ResReplacedStarter.ResShortStarter(oldStarter.get().getId(), oldStarter.get().getWorkflowDefinition().getId());
    }
    entity = starterRepository.save(entity);
    createWorkersForStarter(entity);

    var actual = new ResReplacedStarter.ResShortStarter(entity.getId(), entity.getWorkflowDefinition().getId());
    return new ResReplacedStarter(old, actual);
  }

  public void applySecrets(Starter starter) {
    Set<String> secrets = starter.findSecrets();

    ExternalProperties resolved = null;
    if(!secrets.isEmpty()) {
      ResolveExternalPropertiesConfig config = new ResolveExternalPropertiesConfig().setToResolve(secrets);
      resolved = executor.resolvePropperties(config);
    }

    @NonNull var type = starter.getType();

    switch (type) {
      case Const.StarterType.SAP_INBOUND:
        @NonNull
        SapInboundStarter inbound = starter.getSapInbound();
        inbound.applySecrets(resolved);
        if (starter.getName() == null || starter.getName().isEmpty()) {
          starter.setName(inbound.getInboundDef().getName());
        }
        break;
      case Const.StarterType.SCHEDULER:
        break;
      case Const.StarterType.REST_CALL:
        break;
      case Const.StarterType.KAFKA_CONSUMER:
        applySecrets(starter.getKafkaConsumer(), resolved);
        break;
      case Const.StarterType.RABBITMQ_CONSUMER:
        applySecrets(starter.getRabbitmqConsumer(), resolved);
        break;
      case Const.StarterType.MAIL_CONSUMER:
        applySecrets(starter.getMailConsumer(), resolved);
        break;
      case Const.StarterType.IBMMQ_CONSUMER:
        applySecrets(starter.getIbmmqConsumer(), resolved);
        break;
      default:
        throw new IllegalStateException("unknown starter type: " + type);
    }
  }

  private static void applySecrets(EncryptedDataHandler consumer, ExternalProperties resolved) {
    List<ClientErrorDescription> errors = new ArrayList<>();
    consumer.applySecrets(errors, resolved);
    if(!errors.isEmpty()) {
      throw new ClientError(errors);
    }
  }

  @Override
  public List<StarterShortListValue> search(StarterSearching searching) {
    return starterRepository.search(searching);
  }

  @Override
  @Transactional
  public StarterEntity createStarterAndWorkers(Starter starter) {
    var workflowDefinition =  workflowDefinitionRepositoryHelper.findDeployedDefinition(new Ref().setId(starter.getWorkflowDefinitionToStartId()))
        .orElseThrow(() -> new ClientError(WORKFLOW_IS_NOT_FOUND_BY_REF, new ErrorMessagePouch()));

    var tenantId = workflowDefinition.getTenantId();
    starterRepository.findByTypeAndNameAndTenantId(starter.getType(), starter.getName(), tenantId)
        .ifPresent(starterEntity -> {
          throw ErrorHelper.starterAlreadyExists(starter.getName(), starter.getType());
        });
        var entity = mapper.toStarterEntity(starter, workflowDefinition);
    entity = starterRepository.save(entity);
    createWorkersForStarter(entity);
    return entity;
  }

  @Override
  public void replaceStarter(UUID id, Starter replacedBy) {
    var toReplace = getStarterEntity(id);
    replaceStarter(toReplace, replacedBy);
  }

  @Override
  @Transactional
  public Starter getStarter(UUID id) {
    var entity = getStarterEntity(id);
    var detailedDefinition = jsonSerializer.toDetailedWorkflowDefinition(entity.getWorkflowDefinition());

    var inputValidationSchema = Optional.ofNullable(detailedDefinition.getDetails())
        .map(DefinitionDetails::getInputValidateSchema)
        .orElse(null);
    var starter = mapper.toStarter(entity)
        .setWorkflowInputValidateSchema(inputValidationSchema)
        .setWorkflowDefinitionToStartId(detailedDefinition.getId());
        applySecrets(starter);
    return starter;
  }

  @Override
  @Transactional
  public void softDeleteStarter(UUID id) {
    softDeleteStarter(getStarterEntity(id));
  }

  @Override
  @Transactional
  public void softDeleteStarterByWorkflowDefId(UUID workflowDefinitionToStartId) {
    starterRepository.findByWorkflowDefinitionId(workflowDefinitionToStartId)
        .stream().filter(e -> (!StarterStatus.DELETED.equalsIgnoreCase(e.getActualStatus()) && !StarterStatus.DELETED.equals(e.getDesiredStatus())))
        .forEach(this::softDeleteStarter);
  }

  @Override
  @Transactional
  public void softDeleteStarter(ReqStopStarter stopStarter) {
    var entity =
        starterRepository.findByNameAndTypeAndWorkflowDefinitionId(stopStarter.getName(),
                stopStarter.getType(), stopStarter.getWorkflowId())
            .orElseThrow(() -> ErrorHelper.starterIsNotFound(stopStarter));

    softDeleteStarter(entity);
  }

  private void softDeleteStarter(StarterEntity starter) {
    if (StarterStatus.DELETED.equalsIgnoreCase(starter.getDesiredStatus())
        && StarterStatus.DELETED.equalsIgnoreCase(starter.getActualStatus())) {
      throw ErrorHelper.starterAlreadyDeleted(starter.getId());
    }
    starter.setDesiredStatus(StarterStatus.DELETED);
    starter.setActualStatus(StarterStatus.DELETED);
    workerRepository.deleteWorkersForStarter(starter.getId());
  }

  @Override
  public Long searchCount(StarterSearching searchConfig) {
    return starterRepository.searchCount(searchConfig);
  }

  @Override
  @Transactional
  public void disableExpiredStarters() {
    List<String> statuses =
        List.of(StarterStatus.STARTED, StarterStatus.ERROR, StarterStatus.UNKNOWN);
    var expiredStarters = starterRepository.getExpiredStarters(statuses);
    expiredStarters.forEach(starter -> {
      starter.setDesiredStatus(EXPIRED);
      starter.setActualStatus(EXPIRED);
      workerRepository.deleteWorkersForStarter(starter.getId());
    });
    starterRepository.saveAll(expiredStarters);
  }

  @Override
  @Transactional
  public void updateStarter(UUID id, ReqStarterPatch starterPatch) {
    var currentStarter = getStarterEntity(id);
    starterPatchService.patchStarterEntity(currentStarter, starterPatch);
    starterRepository.save(currentStarter);
    //TODO пересоздавать воркеры если были фактические изменения
    workerRepository.deleteWorkersForStarter(currentStarter.getId());
    createWorkersForStarter(currentStarter);
  }

  private StarterEntity replaceStarter(StarterEntity toReplace, Starter starter) {
    try {
      var workflowDefinition = workflowDefinitionRepositoryHelper.findDeployedDefinition(new Ref().setId(starter.getWorkflowDefinitionToStartId())).orElseThrow();
      return th.inNewTransaction(() -> {
        var toSave = mapper.toStarterEntity(starter, workflowDefinition)
            .setCreateTime(toReplace.getCreateTime())
            .setId(toReplace.getId());
        workerRepository.deleteWorkersForStarter(toReplace.getId());
        createWorkersForStarter(toSave);

        return starterRepository.save(toSave);
      });
    } catch (DataIntegrityViolationException th) {
      throw ErrorHelper.starterAlreadyExists(starter.getName(), starter.getType());
    }
  }

  private void createWorkersForStarter(StarterEntity savedStarter) {
    WorkerEntity worker = new WorkerEntity();
    worker.setStarter(savedStarter);
    workerRepository.save(worker);
  }

  private StarterEntity getStarterEntity(UUID id) {
    return starterRepository.findById(id).orElseThrow(() -> ErrorHelper.starterIsNotFound(id));
  }

}
