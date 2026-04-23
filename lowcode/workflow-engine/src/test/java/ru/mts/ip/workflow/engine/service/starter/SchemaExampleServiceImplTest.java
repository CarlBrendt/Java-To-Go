package ru.mts.ip.workflow.engine.service.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.json.VariablesJsonSchema;

import static org.junit.jupiter.api.Assertions.*;

class SchemaExampleServiceImplTest {

  private SchemaExampleServiceImpl service;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    service = new SchemaExampleServiceImpl(objectMapper);
  }

  @SneakyThrows
  @Test
  void createExampleForSchema() {
    var schema =  objectMapper.readValue(Const.StarterJsonSchema.KAFKA_CONSUMER, VariablesJsonSchema.class);
    var jsonExample = service.createExampleForSchema(schema);
    var actualResult = jsonExample.asNode();
    var expectedResult = "{\"topic\":\"text\",\"key\":\"text\",\"headers\":{},\"payload\":{}}";
    assertEquals(expectedResult, objectMapper.writeValueAsString(actualResult));
  }

  @Test
  @SneakyThrows
  void createExampleForSchema2() {
    var stingSchema = "{\"type\":\"object\",\"properties\":{\"someInfo\":{\"type\":\"object\"}},\"required\":[\"someInfo\"]}";
    var schema =  objectMapper.readValue(stingSchema, VariablesJsonSchema.class);
    var jsonExample = service.createExampleForSchema(schema);
    var actualResult = jsonExample.asNode();
    var expectedResult = "{\"someInfo\":{}}";
    assertEquals(expectedResult, objectMapper.writeValueAsString(actualResult));
  }


}
