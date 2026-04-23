package ru.mts.ip.workflow.engine.service.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpHeaders;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import ru.mts.ip.workflow.engine.controller.WorkflowApiController;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapper;
import ru.mts.ip.workflow.engine.controller.dto.ReqRef;
import ru.mts.ip.workflow.engine.controller.dto.ReqStartWorkflow;
import ru.mts.ip.workflow.engine.entity.SapTaskDetails;
import ru.mts.ip.workflow.engine.entity.StarterEntity;
import ru.mts.ip.workflow.engine.entity.StarterTaskEntity;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.exception.ClientError;
import ru.mts.ip.workflow.engine.repository.StarterRepository;
import ru.mts.ip.workflow.engine.repository.StarterTaskRepository;
import ru.mts.ip.workflow.engine.repository.StarterTaskSpecifications;
import ru.mts.ip.workflow.engine.repository.TransactionHelper;
import ru.mts.ip.workflow.engine.service.blobstorage.BlobRef;
import ru.mts.ip.workflow.engine.service.dto.StarterTask;
import ru.mts.ip.workflow.engine.utility.ErrorHelper;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StarterTaskServiceImpl implements StarterTaskService {

  private final StarterTaskRepository taskRepository;
  private final TransactionHelper th;
  private final WorkflowApiController workflowApiController;
  private final StarterRepository starterRepository;
  private final DtoMapper mapper;
  private final ObjectMapper objectMapper;

  private static final int BATCH_SIZE = 5000;
  private static final int EXPIRE_TASK_MONTHS = 3;


  @Override
  @Transactional
  public StarterTaskEntity save(StarterTask starterTask) {
    StarterTaskEntity entity = mapper.toStarterTaskEntity(starterTask);
    var starterId = Optional.of(starterTask).map(StarterTask::getSapTaskDetails).map(SapTaskDetails::getStarterId).orElseThrow();
    var definition = starterRepository.findById(starterId)
        .map(StarterEntity::getWorkflowDefinition)
        .orElseThrow();
    entity.setWorkflowDefinition(definition);
    return taskRepository.save(entity);
  }

  @Override
  public void processStartWorkflow() {
    boolean continueIteration = true;

    while (continueIteration) {
      var OptionalEntity = getOverdueTaskAndLock(Set.of(StarterTaskEntity.Type.WORKFLOW_START), Set.of(StarterTaskEntity.State.NEW, StarterTaskEntity.State.ERROR));
      if (OptionalEntity.isEmpty()){
        continueIteration = false;
      } else  {
        var taskEntity = OptionalEntity.get();
        th.inNewTransaction(() -> {
          JsonNode starterJson = null;
          String transactionId = null;
          String idoc = null;
          try {
            SapTaskDetails details = taskEntity.getSapTaskDetails();
            WorkflowDefinition workflowDefinition = taskEntity.getWorkflowDefinition();
            transactionId = details.getIdocTID();
            starterJson = Optional.ofNullable(details.getStarterId())
                .flatMap(starterRepository::findById)
                .map(mapper::toStarterLogEntry)
                .map(objectMapper::<JsonNode>valueToTree)
                .orElse(null);

            Map<String, JsonNode> args = new HashMap<>();
            BlobRef blobRef = details.getIdocContentRef();
            String base64Content = details.getIdocContentBase64();
            if (base64Content != null) {
              idoc = new String(Base64.getDecoder().decode(base64Content), StandardCharsets.UTF_8);
            } else {
              idoc = blobRef == null ? null : blobRef.asLowCodeDecorateVariableRef();
            }

            if (idoc == null) {
              throw new IllegalStateException("Empty idoc");
            }
            args.put("idoc", new TextNode(idoc));
            var businessKey =
                details.getIdocNumber() != null ? details.getIdocNumber() : details.getIdocId();

            MDC.put("starter-id",
                Optional.ofNullable(details.getStarterId()).map(UUID::toString).orElse(null));
            MDC.put("workflow-ref-id", workflowDefinition.getId().toString());
            MDC.put("business-key", businessKey);
            MDC.put("tenant-id", workflowDefinition.getTenantId());



            ReqStartWorkflow request = new ReqStartWorkflow().setWorkflowStartConfig(
                    new ReqStartWorkflow.ReqWorkflowStartConfig().setBusinessKey(businessKey)
                        .setVariables(args))
                .setWorkflowRef(new ReqRef().setId(workflowDefinition.getId()));
            //TODO call service instead controller
            workflowApiController.startWorkflowfromSapInbound(
                objectMapper.writeValueAsString(request), true, HttpHeaders.EMPTY);

            taskRepository.save(taskEntity.complete());

            log.info("Consumed starter SAP message. TransactionID: {} IDock: {} Starter: {}",
                transactionId, idoc, starterJson);
          } catch (ClientError ce) {
            log.error("Skipped due to ClientError starter SAP message. TransactionID: {} IDock: {} Starter: {} ErrorMessage: {}" , transactionId , idoc, starterJson, ce.getMessage(), ce);
            taskRepository.save(taskEntity.skip(ce));
          } catch (Exception ex) {
            log.error(
                "Error starter SAP message. TransactionID: {} IDock: {} Starter: {} ErrorMessage: {}",
                transactionId, idoc, starterJson, ex.getMessage(), ex);
            taskRepository.save(taskEntity.error(ex));
          } finally {
            MDC.clear();
          }
        });
      }
    }
  }

  @Override
  @Transactional
  public void stopTask(UUID id) {
   var task =  taskRepository.findById(id).orElseThrow(() -> ErrorHelper.taskIsNotFound(id));
    taskRepository.save(task.manualStop());
  }

  @Override
  @Transactional
  public StarterTaskEntity restartTask(UUID id) {
    var task =  taskRepository.findById(id).orElseThrow(() -> ErrorHelper.taskIsNotFound(id));
    task.setDefaults();
    return taskRepository.save(task);
  }

  @Override
  @Transactional
  public StarterTaskEntity getSapTask(UUID id) {
    return taskRepository.findById(id).orElseThrow(() -> ErrorHelper.taskIsNotFound(id));
  }



  @Override
  @Transactional
  public void deleteOldTasks() {
    OffsetDateTime cutoffDate = OffsetDateTime.now().minusMonths(EXPIRE_TASK_MONTHS);
    taskRepository.deleteOldTasks(cutoffDate, BATCH_SIZE);
  }


  private Optional<StarterTaskEntity> getOverdueTaskAndLock(Set<StarterTaskEntity.Type> types, Set<StarterTaskEntity.State> states) {
    try {
        return th.inNewTransaction(() ->
          taskRepository.findAll(
              StarterTaskSpecifications.findExecutable(types, states),
              PageRequest.of(0, 1, Sort.by(Direction.ASC, "createTime"))
          )
          .stream().findFirst().map(StarterTaskEntity::lock)
        );
    } catch (ObjectOptimisticLockingFailureException ex) {
        return getOverdueTaskAndLock(types, states);
    }
  }

}
