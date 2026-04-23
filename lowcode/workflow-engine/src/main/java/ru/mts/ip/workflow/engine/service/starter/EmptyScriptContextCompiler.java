package ru.mts.ip.workflow.engine.service.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.dto.Starter;
import ru.mts.ip.workflow.engine.json.VariablesJsonSchema;

@Service(Const.ContextCompilerBeans.EMPTY)
@RequiredArgsConstructor
public class EmptyScriptContextCompiler implements ScriptContextCompiler {
  @Override
  public JsonNode compileDefaultVariables(Starter starter) {
    return NullNode.getInstance();
  }

  @Override
  public JsonNode getOutputTemplate(Starter starter){
    return NullNode.getInstance();
  }
}
