package ru.mts.ip.workflow.engine.json;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapper;
import ru.mts.ip.workflow.engine.controller.dto.ReqStarter;
import ru.mts.ip.workflow.engine.dto.DefinitionDetails;
import ru.mts.ip.workflow.engine.dto.DetailedWorkflowDefinition;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.service.Variables;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonSerializerImpl implements JsonSerializer{

  private final ObjectMapper om;
  private final DtoMapper mapper;

  @Override
  public <T> T treeToValue(JsonNode schema, Class<T> class1) {
    try {
      return om.treeToValue(schema, class1);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Variables geneateVariables(JsonNode schema) {
    return Optional.ofNullable(schema)
        .flatMap(this::parseVariablesJsonSchemaIgnoreErrors)
        .map(JsonExample::new)
        .map(JsonExample::asVariables)
        .orElse(new Variables());
  }

  private Optional<VariablesJsonSchema> parseVariablesJsonSchemaIgnoreErrors(JsonNode schema){
    return Optional.of(treeToValue(schema, VariablesJsonSchema.class));
  }

  @Override
  public DetailedWorkflowDefinition toDetailedWorkflowDefinition(WorkflowDefinition definition) {
    DetailedWorkflowDefinition executable = mapper.toExecutableWorkflowDefinition(definition);
    executable.setCompiled(definition.getCompiled());
    executable.setDetails(toWorkflowDetails(definition.getDetails()));
    return executable;
  }
  
  @Override
  public JsonNode toJson(Object object) {
    return om.valueToTree(object);
  }

  public DefinitionDetails toWorkflowDetails(JsonNode json) {
    if(json == null) {
      return null;
    } else {
      try {
        return om.treeToValue(json, DefinitionDetails.class);
      } catch (JsonProcessingException ex) {
        log.error("Invalid WorkflowDetails json present ", ex);
        throw new IllegalStateException(ex);
      }
    }
  }

  @Override
  public List<ReqStarter> toListOfStarters(JsonNode json) {
    return null;
  }

  @Override
  public ArrayNode createArrayNode() {
    return om.createArrayNode();
  }


}
