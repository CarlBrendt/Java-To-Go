package ru.mts.ip.workflow.engine.service.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mts.ip.workflow.engine.configuration.StarterJsonSchemaConfiguration;
import ru.mts.ip.workflow.engine.dto.IbmmqConsumer;
import ru.mts.ip.workflow.engine.dto.Starter;

import static org.junit.jupiter.api.Assertions.assertEquals;


class IbmmqScriptContextCompilerTest {

  private IbmmqScriptContextCompiler contextCompiler;
  private final StarterJsonSchemaConfiguration starterJsonSchemaConfiguration = new StarterJsonSchemaConfiguration();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    var valSchema = starterJsonSchemaConfiguration.ibmmqDefaultValidationSchema(objectMapper);
    var schemaExampleServ = new SchemaExampleServiceImpl(objectMapper);
    contextCompiler = new IbmmqScriptContextCompiler(valSchema, schemaExampleServ);
  }

  @SneakyThrows
  @Test
  void compileDefaultVariables() {
    Starter starter = new Starter();
    IbmmqConsumer consumer = new IbmmqConsumer();
    starter.setIbmmqConsumer(consumer);
    var result = contextCompiler.compileDefaultVariables(starter);
    String expected = "{\"payload\":{},\"queue\":\"text\",\"properties\":{\"priority\":0,\"correlation_id\":\"text\",\"reply_to\":\"text\",\"expiration\":\"text\",\"message_id\":\"text\",\"timestamp\":\"text\",\"type\":\"text\"}}";
    assertEquals(expected, objectMapper.writeValueAsString(result));
  }

  @SneakyThrows
  @Test
  void compileDefaultVariables_withPayloadSchema() {
    Starter starter = new Starter();
    IbmmqConsumer consumer = new IbmmqConsumer();
    consumer.setPayloadValidateSchema(objectMapper.readTree("{}"));
    starter.setIbmmqConsumer(consumer);
    var result = contextCompiler.compileDefaultVariables(starter);
    String expected = "{\"payload\":{},\"queue\":\"text\",\"properties\":{\"priority\":0,\"correlation_id\":\"text\",\"reply_to\":\"text\",\"expiration\":\"text\",\"message_id\":\"text\",\"timestamp\":\"text\",\"type\":\"text\"}}";
    assertEquals(expected, objectMapper.writeValueAsString(result));
  }

  @Test
  void getOutputTemplate() {
  }
}
