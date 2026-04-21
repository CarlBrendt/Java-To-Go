package ru.mts.ip.workflow.engine.dto;

import java.util.List;
import ru.mts.ip.workflow.engine.temporal.RestCallInput.RestCallConfig.AuthDef;


public record DefinitionCompiled(List<CompiledActivity> activities) {


  public record CompiledActivity(CompiledWorkflowCall workflowCall, String type, String id, String name) {
  }


  public record CompiledWorkflowCall(CompiledWorkflowDef workflowDef) {
  }


  public record CompiledWorkflowDef(CompiledWorkflowDefDetails details, String type) {
  }


  public record CompiledWorkflowDefDetails(IntpApi intpApi, CompiledRestCallConfig restCallConfig) {
  }

  public record CompiledRestCallConfig(CompiledRestCallTemplateDef restCallTemplateDef) {
    
  }

  public record CompiledRestCallTemplateDef(AuthDef authDef) {
    
  }

}
