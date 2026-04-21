package ru.mts.ip.workflow.engine.service.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.dto.Starter;
import ru.mts.ip.workflow.engine.json.VariablesJsonSchema;
import ru.mts.ip.workflow.engine.validation.schema.v2.kafka.KafkaMessageSchema;

import java.util.Optional;

@Service(Const.ContextCompilerBeans.KAFKA)
@RequiredArgsConstructor
public class KafkaScriptContextCompiler implements ScriptContextCompiler {
  @Qualifier("kafkaDefaultValidationSchema")
  private final VariablesJsonSchema kafkaDefaultValidationSchema;
  private final SchemaExampleService schemaExample;

  @Override
  public JsonNode compileDefaultVariables(Starter starter) {
    var consumer = starter.getKafkaConsumer();
    var defaultVariables = (ObjectNode) schemaExample.createExampleForSchema(kafkaDefaultValidationSchema).asNode();
    Optional.ofNullable(consumer.getPayloadValidateSchema())
        .flatMap(schemaExample::createExampleForSchema)
        .ifPresent(payloadExample -> {
          defaultVariables.set(KafkaMessageSchema.PAYLOAD, payloadExample.asNode());
        });
    Optional.ofNullable(consumer.getHeadersValidateSchema())
        .flatMap(schemaExample::createExampleForSchema)
        .ifPresent(headersExample -> defaultVariables.set(KafkaMessageSchema.HEADERS, headersExample.asNode()));
    Optional.ofNullable(consumer.getKeyValidateSchema())
        .flatMap(schemaExample::createExampleForSchema)
        .ifPresent(keyExample -> defaultVariables.set(KafkaMessageSchema.KEY, keyExample.asNode()));
    return defaultVariables;
  }

  @Override
  public JsonNode getOutputTemplate(Starter starter){
    return starter.getKafkaConsumer().getOutputTemplate();
  }
}
