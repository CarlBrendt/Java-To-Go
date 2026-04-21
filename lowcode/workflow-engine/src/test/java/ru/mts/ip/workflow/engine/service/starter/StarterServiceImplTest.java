package ru.mts.ip.workflow.engine.service.starter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.configuration.ObjectMapperConfiguration;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapper;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapperImpl;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqStopStarter;
import ru.mts.ip.workflow.engine.dto.IbmmqConsumer;
import ru.mts.ip.workflow.engine.dto.Ref;
import ru.mts.ip.workflow.engine.dto.Starter;
import ru.mts.ip.workflow.engine.entity.StarterEntity;
import ru.mts.ip.workflow.engine.entity.WorkerEntity;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.exception.ClientError;
import ru.mts.ip.workflow.engine.json.JsonSerializer;
import ru.mts.ip.workflow.engine.json.JsonSerializerImpl;
import ru.mts.ip.workflow.engine.repository.StarterRepository;
import ru.mts.ip.workflow.engine.repository.StarterWorkerRepository;
import ru.mts.ip.workflow.engine.repository.TransactionHelper;
import ru.mts.ip.workflow.engine.repository.WorkflowDefinitionRepositoryHelper;
import ru.mts.ip.workflow.engine.service.dto.StarterSearching;
import ru.mts.ip.workflow.engine.service.dto.StarterShortListValue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StarterServiceImplTest {

  @Mock
  private StarterRepository starterRepository;
  @Mock
  private StarterWorkerRepository workerRepository;
  @Mock
  private WorkflowDefinitionRepositoryHelper workflowDefinitionRepositoryHelper;
  @Spy
  private TransactionHelper th = new TransactionHelper();
  @Spy
  private DtoMapper mapper = new DtoMapperImpl();
  @Spy
  private JsonSerializer jsonSerializer = new JsonSerializerImpl(new ObjectMapperConfiguration().objectMapper(), mapper);

  @InjectMocks
  private StarterServiceImpl starterService;

  @BeforeEach
  void setUp() {
  }

  @Test
  void createOrReplaceStarter() {
    final UUID starterId = UUID.fromString("8c214e91-cd2e-4311-8482-9be1f3871026");
    final UUID definitionId = UUID.fromString("7004ca99-3e5b-413c-9286-107dc3a88dbc");
    StarterEntity exist = new StarterEntity();
    exist.setId(starterId);
    exist.setActualStatus(Const.StarterStatus.DELETED);
    exist.setWorkflowDefinition(new WorkflowDefinition().setId(definitionId));
    when(starterRepository.findByTypeAndNameAndTenantId(any(), any(), any())).thenReturn(Optional.of(exist));
    when(starterRepository.save(any())).thenReturn(exist);
    when(workflowDefinitionRepositoryHelper.findDeployedDefinition(any())).thenReturn(Optional.of(new WorkflowDefinition()));

    Starter toReplace = new Starter();
    toReplace.setType(Const.StarterType.IBMMQ_CONSUMER);
    toReplace.setIbmmqConsumer(new IbmmqConsumer());
    var result = starterService.createOrReplaceStarter(toReplace);

    ArgumentCaptor<StarterEntity> captorStarter = ArgumentCaptor.forClass(StarterEntity.class);
    verify(starterRepository).save(captorStarter.capture());
    ArgumentCaptor<WorkerEntity> captorWorker = ArgumentCaptor.forClass(WorkerEntity.class);
    verify(workerRepository).save(captorWorker.capture());
    var starterEntity = captorStarter.getValue();
    starterEntity.initDefaults();
    assertEquals(Const.StarterStatus.STARTED, starterEntity.getDesiredStatus());
    assertEquals(Const.StarterStatus.UNKNOWN, starterEntity.getActualStatus());
    assertEquals(starterId, result.oldStarter().starterId());
    assertEquals(starterId, result.newStarter().starterId());

    var workerEntity = captorWorker.getValue();
    assertNotNull(workerEntity.getStarter());
  }

  @Test
  void search() {
    StarterSearching searching = new StarterSearching();
    StarterShortListValue listValue = new StarterShortListValue().setName("some name");
    when(starterRepository.search(any())).thenReturn(List.of(listValue));
    var result = starterService.search(searching);
    assertEquals(List.of(listValue), result);
  }

  @Test
  void createStarterAndWorkers_when_starter_already_exists() {
    Starter starter = new Starter().setName("name").setType("kafkaConsumer");
    StarterEntity starterEntity = new StarterEntity().setName(starter.getName()).setType(starter.getType());
    WorkflowDefinition def = new WorkflowDefinition().setTenantId("tenantId");
    when(workflowDefinitionRepositoryHelper.findDeployedDefinition(any(Ref.class))).thenReturn(Optional.of(def));
    when(starterRepository.findByTypeAndNameAndTenantId(anyString(), anyString(), anyString())).thenReturn(Optional.of(starterEntity));
    assertThrows(ClientError.class, ()-> starterService.createStarterAndWorkers(starter));
  }

  @Test
  void createStarterAndWorkers_when_starter_not_exists() {
    final UUID starterId = UUID.fromString("8c214e91-cd2e-4311-8482-9be1f3871026");
    Starter starter = new Starter().setName("name").setType("kafkaConsumer");
    when(starterRepository.findByTypeAndNameAndTenantId(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
    when(starterRepository.save(any(StarterEntity.class))).thenAnswer(invocation -> {
      StarterEntity entity = invocation.getArgument(0);
      entity.initDefaults();
      entity.setId(starterId);
      return entity;
    });
    when(workflowDefinitionRepositoryHelper.findDeployedDefinition(any())).thenReturn(Optional.of(new WorkflowDefinition().setTenantId("tenantId")));
    when(starterRepository.findByTypeAndNameAndTenantId(anyString(), anyString(), anyString())).thenReturn(Optional.empty());

    var entity = starterService.createStarterAndWorkers(starter);
    assertEquals(starterId, entity.getId());
    ArgumentCaptor<StarterEntity> captorStarter = ArgumentCaptor.forClass(StarterEntity.class);
    verify(starterRepository).save(captorStarter.capture());
    var starterParam = captorStarter.getValue();
    assertEquals("name", starterParam.getName());
    verify(workerRepository).save(any());
  }

  @Test
  void replaceStarter_when_starter_not_exists() {
    final UUID starterId = UUID.fromString("8c214e91-cd2e-4311-8482-9be1f3871026");
    Starter starter = new Starter().setName("name").setType("kafkaConsumer");
    when(starterRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
    assertThrows(ClientError.class, ()-> starterService.replaceStarter(starterId, starter));
  }

  @Test
  void replaceStarter_when_starter_exists() {
    final UUID starterId = UUID.fromString("8c214e91-cd2e-4311-8482-9be1f3871026");
    Starter starter = new Starter().setName("name").setType("kafkaConsumer");
    StarterEntity starterEntity = new StarterEntity().setId(starterId).setName(starter.getName()).setType(starter.getType());
    when(starterRepository.findById(any(UUID.class))).thenReturn(Optional.of(starterEntity));
    when(starterRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(workflowDefinitionRepositoryHelper.findDeployedDefinition(any())).thenReturn(Optional.of(new WorkflowDefinition()));
    starterService.replaceStarter(starterId, starter);
    verify(starterRepository).save(any());
    verify(workerRepository).save(any());
  }

  @Test
  void getStarter_when_starter_not_exists() {
    final UUID starterId = UUID.fromString("8c214e91-cd2e-4311-8482-9be1f3871026");
    when(starterRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
    assertThrows(ClientError.class, ()-> starterService.getStarter(starterId));
  }

  @Test
  void getStarter_when_starter_exists() {
    final UUID starterId = UUID.fromString("8c214e91-cd2e-4311-8482-9be1f3871026");
    final UUID definitionId = UUID.fromString("7004ca99-3e5b-413c-9286-107dc3a88dbc");

    StarterEntity starterEntity = new StarterEntity().setId(starterId).setName("name").setType("rest_call");
    WorkflowDefinition definition = new WorkflowDefinition().setId(definitionId);
    starterEntity.setWorkflowDefinition(definition);

    when(starterRepository.findById(any(UUID.class))).thenReturn(Optional.of(starterEntity));
    var result = starterService.getStarter(starterId);
    assertEquals("name", result.getName());
    assertEquals("rest_call", result.getType());
    assertEquals(definitionId, result.getWorkflowDefinitionToStartId());
  }

  @Test
  void softDeleteStarter_when_already_deleted() {
    final UUID starterId = UUID.fromString("8c214e91-cd2e-4311-8482-9be1f3871026");

    StarterEntity entity =
        new StarterEntity().setId(starterId)
            .setActualStatus(Const.StarterStatus.DELETED)
            .setDesiredStatus(Const.StarterStatus.DELETED);
    when(starterRepository.findById(any(UUID.class))).thenReturn(Optional.of(entity));
    assertThrows(ClientError.class, () -> starterService.softDeleteStarter(starterId));
  }

  @Test
  void softDeleteStarter_when_not_deleted() {
    final UUID starterId = UUID.fromString("8c214e91-cd2e-4311-8482-9be1f3871026");

    StarterEntity entity =
        new StarterEntity().setId(starterId)
            .setActualStatus(Const.StarterStatus.STARTED)
            .setDesiredStatus(Const.StarterStatus.STARTED);
    when(starterRepository.findById(any(UUID.class))).thenReturn(Optional.of(entity));
    starterService.softDeleteStarter(starterId);
    assertEquals(Const.StarterStatus.DELETED, entity.getActualStatus());
    assertEquals(Const.StarterStatus.DELETED, entity.getDesiredStatus());
    verify(workerRepository).deleteWorkersForStarter(starterId);
  }

  @Test
  void softDeleteStarterByWorkflowDefId() {
    final UUID starterIdFirst = UUID.fromString("8c214e91-cd2e-4311-8482-9be1f3871026");
    final UUID starterIdSecond = UUID.randomUUID();
    final UUID definitionId = UUID.fromString("7004ca99-3e5b-413c-9286-107dc3a88dbc");
    StarterEntity entity = new StarterEntity().setId(starterIdFirst).setActualStatus(Const.StarterStatus.STARTED).setDesiredStatus(Const.StarterStatus.STARTED);
    StarterEntity alreadyDeleted = new StarterEntity().setId(starterIdSecond).setActualStatus(Const.StarterStatus.DELETED).setDesiredStatus(Const.StarterStatus.DELETED);
    when(starterRepository.findByWorkflowDefinitionId(any(UUID.class))).thenReturn(List.of(entity,alreadyDeleted));

    starterService.softDeleteStarterByWorkflowDefId(definitionId);
    assertEquals(Const.StarterStatus.DELETED, entity.getActualStatus());
    assertEquals(Const.StarterStatus.DELETED, entity.getDesiredStatus());
    verify(workerRepository).deleteWorkersForStarter(starterIdFirst);
    verify(workerRepository, never()).deleteWorkersForStarter(starterIdSecond);
  }

  @Test
  void testSoftDeleteStarter_throw_when_not_found() {
    final UUID workflowID = UUID.fromString("8c214e91-cd2e-4311-8482-9be1f3871026");
    final String type = "someType";

    var stopStarter = new ReqStopStarter().setWorkflowId(workflowID).setType(type).setName("someName");

    when(starterRepository.findByNameAndTypeAndWorkflowDefinitionId(anyString(), anyString(), eq(workflowID)))
        .thenReturn(Optional.empty());

   var exception =  assertThrows(ClientError.class, () -> starterService.softDeleteStarter(stopStarter));
    assertEquals(HttpStatus.NOT_FOUND,exception.getStatus());
  }

  @Test
  void searchCount() {
  }

}
