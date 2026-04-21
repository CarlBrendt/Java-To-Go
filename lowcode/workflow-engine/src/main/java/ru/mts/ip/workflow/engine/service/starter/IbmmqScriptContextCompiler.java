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

@Service(Const.ContextCompilerBeans.IBMMQ)
@RequiredArgsConstructor
public class IbmmqScriptContextCompiler implements ScriptContextCompiler {
  @Qualifier("ibmmqDefaultValidationSchema")
  private final VariablesJsonSchema ibmmqDefaultValidationSchema;
  private final SchemaExampleService schemaExample;

  @Override
  public JsonNode compileDefaultVariables(Starter starter) {
    var consumer = starter.getIbmmqConsumer();
    var defaultVariables =
        (ObjectNode) schemaExample.createExampleForSchema(ibmmqDefaultValidationSchema).asNode();
    Optional.ofNullable(consumer.getPayloadValidateSchema())
        .flatMap(schemaExample::createExampleForSchema)
        .ifPresent(payloadExample -> defaultVariables.set(RabbitmqMessageSchema.PAYLOAD,
            payloadExample.asNode()));
    return defaultVariables;
  }

  @Override
  public JsonNode getOutputTemplate(Starter starter){
    return starter.getIbmmqConsumer().getOutputTemplate();
  }
}
