package ru.mts.ip.workflow.engine.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import ru.mts.ip.workflow.engine.service.Variables;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VariableValidatorImplTest {

  private VariableValidatorImpl variableValidator;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @SneakyThrows
  void validateStringVariables() {
    variableValidator = new VariableValidatorImpl();
    String jsonString = """
        {"a":{"b":{"c":"d"}}}
        """;
    var node = objectMapper.readTree(jsonString);
    var result = variableValidator.validateStringVariables(List.of("a.b.c"), new Variables(node));

    assertEquals(0, result.size());
  }

  @Test
  @SneakyThrows
  void validateStringVariables_textVariable() {
    variableValidator = new VariableValidatorImpl();
    String jsonString = """
        {"a":{"b":{"c":"d"}}}
        """;
    var node = objectMapper.readTree(jsonString);
    var result = variableValidator.validateStringVariables(List.of("a"), new Variables(node));
    assertEquals(1, result.size());
    jsonString = """
        {"a":"textValue"}
        """;
    node = objectMapper.readTree(jsonString);
    result = variableValidator.validateStringVariables(List.of("a"), new Variables(node));
    assertEquals(0, result.size());
  }

  @Test
  @SneakyThrows
  void validateStringVariables_noExistsPath() {
    variableValidator = new VariableValidatorImpl();
    String jsonString = """
        {"a":{"b":{"c":"d"}}}
        """;
    var node = objectMapper.readTree(jsonString);
    var result = variableValidator.validateStringVariables(List.of("a.b.failPath"), new Variables(node));

    assertEquals(1, result.size());
    System.out.println(result.get(0));
    assertEquals("a.b.failPath", result.get(0).getInputValidationContext().getPropertyViolations().get(0).getPropertyName());
  }


  @Test
  @SneakyThrows
  void validateStringVariables_invalidPath() {
    variableValidator = new VariableValidatorImpl();
    String jsonString = """
        {"a":{"b":{"c":"d"}}}
        """;
    var node = objectMapper.readTree(jsonString);
    var result = variableValidator.validateStringVariables(List.of("?>dfwfgv...fwfw"), new Variables(node));

    assertEquals(1, result.size());
    assertEquals("Character '.' on position 12 is not valid.", result.get(0).getInputValidationContext().getPropertyViolations().get(0).getSystemMessage());
  }
}
