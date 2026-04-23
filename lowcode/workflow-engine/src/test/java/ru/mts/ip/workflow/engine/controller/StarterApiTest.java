package ru.mts.ip.workflow.engine.controller;

import lombok.SneakyThrows;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.mts.ip.workflow.engine.EngineConfigurationProperties;
import ru.mts.ip.workflow.engine.InternationalizerImpl;
import ru.mts.ip.workflow.engine.WikiErrorPageProvider;
import ru.mts.ip.workflow.engine.configuration.ObjectMapperConfiguration;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapperImpl;
import ru.mts.ip.workflow.engine.controller.dto.ResReplacedStarter;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqStarterV2;
import ru.mts.ip.workflow.engine.dto.Starter;
import ru.mts.ip.workflow.engine.entity.StarterEntity;
import ru.mts.ip.workflow.engine.json.JsonSerializerImpl;
import ru.mts.ip.workflow.engine.lang.plant.PlantCompiler;
import ru.mts.ip.workflow.engine.service.blobstorage.InMemoryBlobStorage;
import ru.mts.ip.workflow.engine.service.starter.StarterService;
import ru.mts.ip.workflow.engine.testutils.TestControllerBodies;
import ru.mts.ip.workflow.engine.validation.ErrorCompiler;
import ru.mts.ip.workflow.engine.validation.ValidationService;
import ru.mts.ip.workflow.engine.validation.schema.v2.StarterSchemaV2;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.mts.ip.workflow.engine.controller.dto.ResReplacedStarter.ResShortStarter;

@ActiveProfiles("testing")
@WebMvcTest(value = StarterApi.class)
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
    InMemoryBlobStorage.class
})
class StarterApiTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ValidationService validationService;
  @MockBean
  private StarterService starterService;


  @BeforeEach
  void setUp() {
  }

  @SneakyThrows
  @Test
  void createOrReplaceStarter() {
    UUID starterId = UUID.fromString("1a54c23c-d7bb-4b58-a23c-df69f4187381");
    UUID oldWorkflowId = UUID.fromString("8c76fe51-55ce-4bef-ad42-150062a18a4b");
    UUID newWorkflowId = UUID.fromString("abcaff54-e732-481b-9560-488819aa03d8");
    when(starterService.createOrReplaceStarter(any())).thenReturn(new ResReplacedStarter(new ResShortStarter(starterId, oldWorkflowId), new ResShortStarter(starterId, newWorkflowId)));
    mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/starters")
        .content(TestControllerBodies.SAP_STARTER_CREATE))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(" {\"oldStarter\": {\"starterId\":\"" + starterId + "\",\"workflowId\":\"" + oldWorkflowId + "\" }, \"newStarter\": {\"starterId\":\"" + starterId + "\", \"workflowId\":\"" + newWorkflowId + "\" }}"));
  verify(validationService).valid(anyString(), any(StarterSchemaV2.class), eq(ReqStarterV2.class));
  }

  @Test
  @SneakyThrows
  void createStarter() {
    final String starterId = "1a54c23c-d7bb-4b58-a23c-df69f4187381";
    when(starterService.createStarterAndWorkers(any())).thenReturn(new StarterEntity().setId(UUID.fromString(starterId)));
    mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/starters")
            .content(TestControllerBodies.SAP_STARTER_CREATE))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(" {\"id\":\"" + starterId + "\" }"));
  }

  @Test
  @SneakyThrows
  void replaceStarter() {
    final String starterId = "1a54c23c-d7bb-4b58-a23c-df69f4187381";
    mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/starters/" + starterId)
        .content(TestControllerBodies.SAP_STARTER_CREATE))
        .andExpect(status().isNoContent());
  }

  @Test
  @SneakyThrows
  void getStarter() {
    final String starterId = "1a54c23c-d7bb-4b58-a23c-df69f4187381";
    final String definitionId ="8c76fe51-55ce-4bef-ad42-150062a18a4b";
    Starter starter = new Starter()
                      .setId(UUID.fromString(starterId))
        .setWorkflowDefinitionToStartId(UUID.fromString(definitionId));

    String expected = " {\"id\":\"" + starterId + "\", \"workflowDefinitionToStartId\":\"" + definitionId + "\" }";

    when(starterService.getStarter(eq(UUID.fromString(starterId)))).thenReturn(starter);
    mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/starters/" + starterId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(expected));
  }

  @Test
  @SneakyThrows
  void deleteStarter() {
    final String starterId = "1a54c23c-d7bb-4b58-a23c-df69f4187381";
    mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/starters/" + starterId))
        .andExpect(status().isNoContent());
  }

  @Test
  void testDeleteStarter() {
  }

  @Test
  void findStarters() {
  }

  @Test
  void findStartersCount() {
  }
}
