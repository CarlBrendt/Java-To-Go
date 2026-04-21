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

@Service(Const.ContextCompilerBeans.RABBITMQ)
@RequiredArgsConstructor
public class RabbitmqScriptContextCompiler implements ScriptContextCompiler {
  @Qualifier("rabbitmqDefaultValidationSchema")
  private final VariablesJsonSchema rabbitmqDefaultValidationSchema;
  private final SchemaExampleService schemaExample;

  @Override
  public JsonNode compileDefaultVariables(Starter starter) {
    var consumer = starter.getRabbitmqConsumer();
    var defaultVariables =
        (ObjectNode) schemaExample.createExampleForSchema(rabbitmqDefaultValidationSchema).asNode();
    Optional.ofNullable(consumer.getPayloadValidateSchema())
        .flatMap(schemaExample::createExampleForSchema)
        .ifPresent(payloadExample -> defaultVariables.set(RabbitmqMessageSchema.PAYLOAD,
            payloadExample.asNode()));
    Optional.ofNullable(consumer.getHeadersValidateSchema())
        .flatMap(schemaExample::createExampleForSchema)
        .ifPresent(headersExample -> defaultVariables.set(RabbitmqMessageSchema.HEADERS,
            headersExample.asNode()));
    return defaultVariables;
  }

  @Override
  public JsonNode getOutputTemplate(Starter starter){
    return starter.getRabbitmqConsumer().getOutputTemplate();
  }
}
