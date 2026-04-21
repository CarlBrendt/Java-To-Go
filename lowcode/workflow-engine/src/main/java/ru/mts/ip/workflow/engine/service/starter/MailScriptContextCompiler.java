package ru.mts.ip.workflow.engine.service.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.dto.Starter;
import ru.mts.ip.workflow.engine.json.VariablesJsonSchema;
import ru.mts.ip.workflow.engine.validation.schema.v2.rabbitmq.RabbitmqMessageSchema;

import java.util.Optional;

@Service(Const.ContextCompilerBeans.MAIL)
@RequiredArgsConstructor
public class MailScriptContextCompiler implements ScriptContextCompiler {
  @Qualifier("mailDefaultValidationSchema")
  private final VariablesJsonSchema mailDefaultValidationSchema;
  private final SchemaExampleService schemaExample;

  @Override
  public JsonNode compileDefaultVariables(Starter starter) {
    return schemaExample.createExampleForSchema(mailDefaultValidationSchema).asNode();
  }

  @Override
  public JsonNode getOutputTemplate(Starter starter){
    return starter.getMailConsumer().getOutputTemplate();
  }
}
