package ru.mts.ip.workflow.engine.lang;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.lang.plant.WorkflowExpression;
import ru.mts.ip.workflow.engine.service.WorkflowInstance;

public interface DSLCompiler {

  JsonNode compileWorkflowExpressionToJson(String src);

  WorkflowExpression compileWorkflowExpression(String src);

  String decompileWorkflowExpression(JsonNode compiled);

  WorkflowDefinition compileWorkflowDefinition(String src);

  String decompileWorkflowDefinition(WorkflowDefinition def);

  String decompileWorkflowInstance(WorkflowInstance inst);

}
