package ru.mts.ip.workflow.engine.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import lombok.SneakyThrows;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.EngineConfigurationProperties;
import ru.mts.ip.workflow.engine.InternationalizerImpl;
import ru.mts.ip.workflow.engine.WikiErrorPageProvider;
import ru.mts.ip.workflow.engine.configuration.ObjectMapperConfiguration;
import ru.mts.ip.workflow.engine.controller.dto.DtoCredentialFilter;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapperImpl;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateWorkflowDefinition;
import ru.mts.ip.workflow.engine.controller.dto.ReqStartWorkflow;
import ru.mts.ip.workflow.engine.controller.dto.ReqStopWorkflow;
import ru.mts.ip.workflow.engine.dto.DetailedWorkflowDefinition;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.exception.ErrorDescription;
import ru.mts.ip.workflow.engine.executor.WorkflowExecutorService;
import ru.mts.ip.workflow.engine.json.JsonSerializerImpl;
import ru.mts.ip.workflow.engine.lang.plant.Plant;
import ru.mts.ip.workflow.engine.lang.plant.PlantCompiler;
import ru.mts.ip.workflow.engine.service.WorkflowExecutionResult;
import ru.mts.ip.workflow.engine.service.WorkflowInstance;
import ru.mts.ip.workflow.engine.service.WorkflowService;
import ru.mts.ip.workflow.engine.service.access.AccessService;
import ru.mts.ip.workflow.engine.service.blobstorage.InMemoryBlobStorage;
import ru.mts.ip.workflow.engine.service.starter.StarterService;
import ru.mts.ip.workflow.engine.service.starter.WorkerService;
import ru.mts.ip.workflow.engine.temporal.WorkflowInstanceSearchListValue;
import ru.mts.ip.workflow.engine.validation.ErrorCompiler;
import ru.mts.ip.workflow.engine.validation.ValidationAndParseResult;
import ru.mts.ip.workflow.engine.validation.ValidationResult;
import ru.mts.ip.workflow.engine.validation.ValidationService;

@ActiveProfiles("testing")
@WebMvcTest(value = WorkflowApiController.class)
@ImportAutoConfiguration(exclude = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class})
@Import({
    ErrorCompiler.class,
    DtoMapperImpl.class,
    InternationalizerImpl.class,
    WikiErrorPageProvider.class,
    EngineConfigurationProperties.class,
    ObjectMapperConfiguration.class,
    JsonSerializerImpl.class,
    GlobalControllerExceptionHandler.class,
    PlantCompiler.class,
    InMemoryBlobStorage.class,
})
class WorkflowApiControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ValidationService validationService;
  @MockBean
  private WorkflowService workflowService;
  @MockBean
  private StarterService starterService;
  @MockBean
  private AccessService accessService;
  @MockBean
  private WorkflowExecutorService workflowExecutorService;
  @MockBean
  private DtoCredentialFilter dtoCredentialFilter;
  @MockBean
  private WorkerService workerService;

  @BeforeEach
  void setUp() {
  }

  @SneakyThrows
  @Test
  void createDraft() {
   final UUID uuid = UUID.randomUUID();
   var wfDef = new WorkflowDefinition().setId(uuid).setName("name").setTenantId("tenantId").setVersion(1);
    when(workflowService.createDraftDefinition(any())).thenReturn(wfDef);
    mockMvc.perform(post("/api/v1/wf/draft").content("{}"))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("{\"id\":\"" + uuid + "\",\"name\":\"name\",\"tenantId\":\"tenantId\",\"version\":1}"));
  }

  @SneakyThrows
  @Test
  void replaceDraft() {
    final UUID uuid = UUID.randomUUID();
    var wfDef = new WorkflowDefinition().setId(uuid).setName("name").setTenantId("tenantId").setVersion(1);
    when(workflowService.replaceDraftDefinition(eq(uuid),any())).thenReturn(wfDef);
    mockMvc.perform(put("/api/v1/wf/draft/"+ uuid).content("{}"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("{\"id\":\"" + uuid + "\",\"name\":\"name\",\"tenantId\":\"tenantId\",\"version\":1}"));
  }

  @SneakyThrows
  @Test
  void deployDefinition_badRequest_when_critical_error() {
    ValidationAndParseResult<ReqCreateWorkflowDefinition> parseResult =
        new ValidationAndParseResult<ReqCreateWorkflowDefinition>().setValidationResult(new ValidationResult(new TextNode(""), List.of(new ErrorDescription().setLevel(Const.ErrorLevel.CRITICAL))));

    when(validationService.validateAndParse(any(),any(),eq(ReqCreateWorkflowDefinition.class))).thenReturn(parseResult);

    mockMvc.perform(post("/api/v1/wf/definition").content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @SneakyThrows
  @Test
  void deployDefinition_badRequest_when_parsedBody_has_error() {
    ValidationAndParseResult<ReqCreateWorkflowDefinition> parseResult =
        new ValidationAndParseResult<ReqCreateWorkflowDefinition>().setParseResult(new ReqCreateWorkflowDefinition()).setValidationResult(new ValidationResult(new TextNode(""),new ArrayList<>()));
    when(validationService.validateAndParse(any(),any(),eq(ReqCreateWorkflowDefinition.class))).thenReturn(parseResult);
    ValidationResult criticalValidationResult = new ValidationResult(new TextNode(""), List.of(new ErrorDescription().setLevel(Const.ErrorLevel.CRITICAL)));
    when(validationService.validateRuntime(any(DetailedWorkflowDefinition.class))).thenReturn(criticalValidationResult);

    mockMvc.perform(post("/api/v1/wf/definition").content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @SneakyThrows
  @Test
  void deployDefinition_badRequest_when_notIgnoreWarnings_and_warn_exists() {
    List<ErrorDescription> errors = new ArrayList<>();
    errors.add(new ErrorDescription().setLevel(Const.ErrorLevel.WARNING));
    ValidationAndParseResult<ReqCreateWorkflowDefinition> parseResult =
        new ValidationAndParseResult<ReqCreateWorkflowDefinition>().setParseResult(new ReqCreateWorkflowDefinition()).setValidationResult(new ValidationResult(new TextNode(""),errors));

    when(validationService.validateAndParse(any(),any(),eq(ReqCreateWorkflowDefinition.class))).thenReturn(parseResult);
    ValidationResult criticalValidationResult = new ValidationResult(new TextNode(""), new ArrayList<>());
    when(validationService.validateRuntime(any(DetailedWorkflowDefinition.class))).thenReturn(criticalValidationResult);

    mockMvc.perform(post("/api/v1/wf/definition").queryParam("ignoreWarnings","false").content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @SneakyThrows
  @Test
  void deployDefinition_Ok_when_IgnoreWarnings_and_warn_exists() {
    List<ErrorDescription> errors = new ArrayList<>();
    errors.add(new ErrorDescription().setLevel(Const.ErrorLevel.WARNING));
    ValidationAndParseResult<ReqCreateWorkflowDefinition> parseResult =
        new ValidationAndParseResult<ReqCreateWorkflowDefinition>().setParseResult(new ReqCreateWorkflowDefinition()).setValidationResult(new ValidationResult(new TextNode(""),errors));

    when(validationService.validateAndParse(any(),any(),eq(ReqCreateWorkflowDefinition.class))).thenReturn(parseResult);
    ValidationResult criticalValidationResult = new ValidationResult(new TextNode(""), new ArrayList<>());
    when(validationService.validateRuntime(any(DetailedWorkflowDefinition.class))).thenReturn(criticalValidationResult);

    final UUID uuid = UUID.randomUUID();
    var wfDef = new WorkflowDefinition().setId(uuid).setName("name").setTenantId("tenantId").setVersion(1);
    when(workflowService.deploy(any(),anyBoolean())).thenReturn(wfDef);

    mockMvc.perform(post("/api/v1/wf/definition").content("{}"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("{\"id\":\"" + uuid + "\",\"name\":\"name\",\"tenantId\":\"tenantId\",\"version\":1}"));
  }


  @SneakyThrows
  @Test
  void deployDefinitionByPlant_400_when_invalidPlant() {

    mockMvc.perform(post("/api/v1/wf/definition/plant-uml").contentType(MediaType.TEXT_PLAIN).content("invalid body with plant"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @SneakyThrows
  @Test
  void deployDefinitionByPlant_200() {
    ValidationResult validationResult = new ValidationResult(new TextNode(""), new ArrayList<>());
    when(validationService.validateRuntime(any(DetailedWorkflowDefinition.class))).thenReturn(validationResult);

    final UUID uuid = UUID.randomUUID();
    var wfDef = new WorkflowDefinition().setId(uuid).setName("name").setTenantId("tenantId").setVersion(1);
    when(workflowService.deploy(any(),anyBoolean())).thenReturn(wfDef);

    mockMvc.perform(post("/api/v1/wf/definition/plant-uml").contentType(MediaType.TEXT_PLAIN).content(Plant.EXAMPLE))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @SneakyThrows
  @Test
  void findWorkflowInstancePlant_404() {
    String businessKey = "businessKey";
    when(workflowService.getInstance(any())).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/v1/wf/instance/plant-uml/{businessKey}", businessKey))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @SneakyThrows
  @Test
  void findWorkflowInstancePlant_200() {
    String businessKey = "businessKey";
    var wfDef = new WorkflowDefinition().setId(UUID.randomUUID());
    wfDef.setCompiled(new ObjectMapper().readTree("{\"activities\":[{\"id\":\"activityId\"}]}"));
    var wfInstance = new WorkflowInstance();
    wfInstance.setDef(wfDef);
    when(workflowService.getInstance(any())).thenReturn(Optional.of(wfInstance));

    mockMvc.perform(get("/api/v1/wf/instance/plant-uml/{businessKey}", businessKey))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.valueOf("text/plain;charset=UTF-8")));
  }

  @SneakyThrows
  @Test
  void getDefinitionDraftById_200() {
    final UUID wfDraftId = UUID.randomUUID();
    var wfDef = new WorkflowDefinition().setId(wfDraftId);
    when(workflowService.findDraftById(any())).thenReturn(wfDef);

    mockMvc.perform(get("/api/v1/wf/draft/{id}", wfDraftId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("{\"id\": \"%s\"}".formatted(wfDraftId.toString())));
  }

  @Test
  @SneakyThrows
  void getDeployedDefinitionByIdPlant_200() {
    UUID uuid = UUID.randomUUID();

    var wfDef = new WorkflowDefinition().setTenantId("tenantId").setId(uuid)
        .setCompiled(new ObjectMapper().readTree("{\"activities\":[{\"id\":\"activityId\"}]}"));

    when(workflowService.findDeployedById(any())).thenReturn(wfDef);
    mockMvc.perform(get("/api/v1/wf/definition/plant-uml/{id}", uuid))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.valueOf("text/plain;charset=UTF-8")));
    verify(workflowService).findDeployedById(uuid);
  }

  @Test
  @SneakyThrows
  void startWorkflowInstanceAsync_409() {
    when(validationService.valid(anyString(), any(), any(), anyBoolean())).thenReturn(
        new ReqStartWorkflow().setWorkflowStartConfig(new ReqStartWorkflow.ReqWorkflowStartConfig()));
    when(workflowService.findDeployedDefinition(any())).thenReturn(
        Optional.of(new WorkflowDefinition().setId(UUID.randomUUID())));
    when(workflowService.start(any(), any())).thenThrow(
        new WorkflowExecutionAlreadyStarted(WorkflowExecution.getDefaultInstance(), "wfType",
            new RuntimeException("test")));
    mockMvc.perform(post("/api/v1/wf/start").content("{}"))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @Test
  @SneakyThrows
  void startWorkflowInstanceAsync_200() {
    when(validationService.valid(anyString(), any(), any(), anyBoolean())).thenReturn(
        new ReqStartWorkflow().setWorkflowStartConfig(new ReqStartWorkflow.ReqWorkflowStartConfig()));

    when(workflowService.findDeployedDefinition(any())).thenReturn(
        Optional.of(new WorkflowDefinition().setId(UUID.randomUUID())));
    when(workflowService.start(any(), any())).thenReturn(new WorkflowExecutionResult(null, null, null));
    mockMvc.perform(post("/api/v1/wf/start").content("{}"))
        .andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  void stopWorkflowInstanceAsync() {
    var req = new ReqStopWorkflow();
    req.setBusinessKey("businessKey");
    req.setTerminate(false);
    when(validationService.valid(anyString(), any(), any(), anyBoolean())).thenReturn(req);
    when(workflowService.stop(any(),anyString(),anyBoolean()))
        .thenReturn(List.of(new WorkflowInstanceSearchListValue().setRunId("runId")));

    mockMvc.perform(post("/api/v1/wf/stop").content("{}"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("{\"total\": 1}"))
    ;
  }

}
