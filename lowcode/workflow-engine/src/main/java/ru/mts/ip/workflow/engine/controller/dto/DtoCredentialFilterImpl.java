package ru.mts.ip.workflow.engine.controller.dto;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import ru.mts.ip.workflow.engine.EngineConfigurationProperties;
import ru.mts.ip.workflow.engine.dto.DefinitionDetails;
import ru.mts.ip.workflow.engine.dto.DetailedWorkflowDefinition;
import ru.mts.ip.workflow.engine.dto.Starter;
import ru.mts.ip.workflow.engine.executor.WorkflowExecutorService;

@Component
@RequiredArgsConstructor
public class DtoCredentialFilterImpl implements DtoCredentialFilter{
  
  private final EngineConfigurationProperties props;
  private final WorkflowExecutorService executor;
  
  @Override
  public DetailedWorkflowDefinition filter(DetailedWorkflowDefinition definition) {
    JsonNode compiled = definition.getCompiled();
    if(compiled != null) {
      definition.setCompiled(executor.filterCompiled(compiled));
    }
    if(props.isSecurityClientCredentialsEnabled()) {
      definition.setDetails(filterDefinitionDetails(definition.getDetails()));
    }
    
    return definition;
  }
  
  private DefinitionDetails filterDefinitionDetails(DefinitionDetails details) {
    Optional.ofNullable(details).map(DefinitionDetails::getStarters).orElse(List.of()).forEach(Starter::removeCredentials);
    return details;
  }
  
}
