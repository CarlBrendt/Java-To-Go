package ru.mts.ip.workflow.engine.validation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.EngineConfigurationProperties;
import ru.mts.ip.workflow.engine.dto.Ref;
import ru.mts.ip.workflow.engine.dto.XsdValidation.XsdImport;
import ru.mts.ip.workflow.engine.exception.ErrorDescription;
import ru.mts.ip.workflow.engine.executor.WorkflowExecutorService;
import ru.mts.ip.workflow.engine.repository.WorkflowDefinitionRepositoryHelper;
import ru.mts.ip.workflow.engine.service.XsdService;
import ru.mts.ip.workflow.engine.service.scripting.ScriptExecutorService;
import ru.mts.ip.workflow.engine.service.starter.StarterValidationService;

import static ru.mts.ip.workflow.engine.dto.XsdValidation.*;

@Slf4j
@RequiredArgsConstructor
public class Context {

  public boolean exists(String id) {
    return contains(id);
  }
  
  @Setter
  private JsonNode workflowInputValidateSchema;
  @Setter
  private JsonNode workflowInputXsdValidateSchemaImports;
  @Setter
  private JsonNode workflowInputXsdValidateSchemaVariables;
  private Map<String, List<JsonNode>> allActivities = new HashMap<>();
  private Map<String, List<JsonNode>> allDebugConfigurations = new HashMap<>();
  
  private final List<ErrorDescription> errors = new ArrayList<>();
  @Getter @Setter
  private String tenantId = Const.DEFAULT_TENANT_ID;
  private final ObjectMapper om;
  private final StarterValidationService starterValidationService;
  private final WorkflowDefinitionRepositoryHelper workflowDefinitionRepositoryHelper;
  private final WorkflowExecutorService executionService;
  private final EngineConfigurationProperties props;
  private final ScriptExecutorService scriptExecutorService;
  private final XsdService xsdService;

  public void setActivities(Map<String, List<JsonNode>> allActivities) {
    this.allActivities = allActivities;
  }
  
  public Duration getSyncStartTimoutLimit() {
    return Duration.ofSeconds(props.getSyncStartTimeoutLimitSeconds());
  }
  
  public void setDebugConfigurations(Map<String, List<JsonNode>> allDebugConfigurations) {
    this.allDebugConfigurations = allDebugConfigurations;
  }

  public boolean activityConfigurationExists(String activityId) {
    return allDebugConfigurations.containsKey(activityId);
  }
  
  public Optional<JsonNode> findById(String id) {
    var listsOfActivities = allActivities.get(id);
    if(listsOfActivities == null) {
      return Optional.empty();
    } else if(listsOfActivities.size() == 1){
      return Optional.ofNullable(listsOfActivities.get(0));
    } else {
      throw new IllegalStateException();
    }
  }

  public boolean contains(String id) {
    return  allActivities.get(id) != null;
  }
  
  public boolean isUnique(String id) {
    var listsOfActivities = allActivities.get(id);
    if(listsOfActivities == null || listsOfActivities.isEmpty()) {
      throw new IllegalStateException();
    } else {
      return listsOfActivities.size() == 1;
    }
  }
  
  public void addError(ErrorDescription errorDescription) {
    errors.add(errorDescription);
  }
  
  List<ErrorDescription> validateWorkflowExpression(JsonNode json) {
    return executionService.validateExpression(json).getErrors();
  }

  List<ErrorDescription> validateWorkflowExpressionForEsqlCompilation(JsonNode json) {
    return executionService.validateExpressionForEsqlCompilation(json).getErrors();
  }

  void tryCreteaXsdSchema(String rootSchema) throws SAXException {
    xsdService.creteSchema(rootSchema, compileXsdImports());
  }
  
  private List<XsdImport> compileXsdImports(){
    if(workflowInputXsdValidateSchemaImports != null) {
      if(workflowInputXsdValidateSchemaImports.isArray()) {
        try {
          return om.treeToValue(workflowInputXsdValidateSchemaImports, om.getTypeFactory().constructCollectionType(List.class,  XsdImport.class));
        } catch (JacksonException ignore) {

        }

      }
    }
    return List.of();
  }

  private List<VariableToValidate> compileXsdVariables(){
    if(workflowInputXsdValidateSchemaVariables != null) {
      if(workflowInputXsdValidateSchemaVariables.isArray()) {
        try {
          return om.treeToValue(workflowInputXsdValidateSchemaVariables, om.getTypeFactory().constructCollectionType(List.class,  VariableToValidate.class));
        } catch (JacksonException ignore) {
        }
      }
    }
    return List.of();
  }

  List<ErrorDescription> validateStarterCompatibleWithDefinition(JsonNode starter) {
   var variablesToValidate =  compileXsdVariables();
    return starterValidationService.validateStarterCompatibleWithDefinition(starter, workflowInputValidateSchema, variablesToValidate);
  }

  boolean workflowExistsByRef(JsonNode json) {
    try {
      var ref = om.treeToValue(json, Ref.class);
      if(ref.getId() != null || ref.getName() != null) {
        return workflowDefinitionRepositoryHelper.findDeployedDefinition(ref).isPresent();
      }
    } catch (JsonProcessingException e) {
      log.error("Invalid json Ref ", e);
    } 
    return true;
  }

  public List<ErrorDescription> validateActivity(JsonNode v) {
    return executionService.validateActivity(v).getErrors();
  }

  boolean isExecutable(String text) {
    return scriptExecutorService.isExecutable(text);
  }

  boolean isSecretRef(String text) {
    return text != null && text.startsWith("secret{");
  }

}
