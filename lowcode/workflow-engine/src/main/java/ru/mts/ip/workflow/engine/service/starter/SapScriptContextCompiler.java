package ru.mts.ip.workflow.engine.service.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.dto.Starter;
import ru.mts.ip.workflow.engine.json.VariablesJsonSchema;

@Service(Const.ContextCompilerBeans.SAP)
@RequiredArgsConstructor
public class SapScriptContextCompiler implements ScriptContextCompiler {
  @Qualifier("sapDefaultValidationSchema")
  private final VariablesJsonSchema sapDefaultValidationSchema;
  private final SchemaExampleService schemaExample;

  @Override
  public JsonNode compileDefaultVariables(Starter starter) {
    return schemaExample.createExampleForSchema(sapDefaultValidationSchema).asNode();
  }

  @Override
  public JsonNode getOutputTemplate(Starter starter){
    return NullNode.getInstance();
  }
}
