package ru.mts.ip.workflow.engine.service.xsd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mts.ip.workflow.engine.dto.XsdValidation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class XsdServiceImplTest {

  @Spy
  private ObjectMapper objectMapper = new ObjectMapper();
  @InjectMocks
  private XsdServiceImpl xsdService;

  @Test
  void validateJsonWithXml_no_path_found() {

   JsonNode emptyNode = objectMapper.createObjectNode();
    XsdValidation xsdValidation = new XsdValidation();

    XsdValidation.VariableToValidate variableToValidate = new XsdValidation.VariableToValidate();
    variableToValidate.setVariableName("absentVariable");
    variableToValidate.setXsdSchemaBase64Content("PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHhzOnNjaGVtYSB4bWxuczp4cz0iaHR0cDovL3d3dy53My5vcmcvMjAwMS9YTUxTY2hlbWEiPgogICAgPHhzOmVsZW1lbnQgbmFtZT0ibWVzc2FnZSIgdHlwZT0ieHM6c3RyaW5nIi8+CjwveHM6c2NoZW1hPg==");
    xsdValidation.setVariablesToValidate(List.of(variableToValidate));
   var result = xsdService.validateJsonWithXml(emptyNode, xsdValidation);
   assertEquals(1 , result.size());
   var errDescription = result.get(0);
   assertEquals("No results for path: $['absentVariable']", errDescription.getSystemMessage());
  }
}
