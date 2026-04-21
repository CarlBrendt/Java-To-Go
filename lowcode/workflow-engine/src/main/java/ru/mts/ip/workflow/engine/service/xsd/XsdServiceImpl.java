package ru.mts.ip.workflow.engine.service.xsd;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import ru.mts.ip.workflow.engine.Const.Errors2;
import ru.mts.ip.workflow.engine.dto.InputValidationContext;
import ru.mts.ip.workflow.engine.dto.InputValidationContext.PropertyViolation;
import ru.mts.ip.workflow.engine.dto.XsdValidation;
import ru.mts.ip.workflow.engine.dto.XsdValidation.VariableToValidate;
import ru.mts.ip.workflow.engine.dto.XsdValidation.XsdImport;
import ru.mts.ip.workflow.engine.exception.ClientErrorDescription;
import ru.mts.ip.workflow.engine.service.XsdService;

@Slf4j
@Service
@RequiredArgsConstructor
public class XsdServiceImpl implements XsdService {

  private final ObjectMapper objectMapper;

  @Override
  public Schema creteSchema(String rootSchema, List<XsdImport> imports) throws SAXException {
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    factory.setResourceResolver(new CustomResourceResolver(imports));
    return factory.newSchema(new StreamSource(new StringReader(rootSchema)));
  }

  @Override
  public List<ClientErrorDescription> validateJsonWithXml(JsonNode jsonNode, XsdValidation xsdValidation) {
    List<ClientErrorDescription> errors = new ArrayList<>();
    List<XsdImport> imports = xsdValidation.getImports();
    @NonNull List<VariableToValidate> toValiateVariables = xsdValidation.getVariablesToValidate();
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    if(imports != null) {
      factory.setResourceResolver(new CustomResourceResolver(imports));
    }
    
    for(var toValidate : toValiateVariables) {
      if(errors.isEmpty()) {
        @NonNull String varName = toValidate.getVariableName();
        @NonNull String xsdBase64 = toValidate.getXsdSchemaBase64Content();
        String xsd = decodeBase64(xsdBase64);


        DocumentContext context = JsonPath.parse(jsonNode.toString());

        JsonNode varValue = null;
        String systemMessage = null;
        try {
          varValue = objectMapper.convertValue(context.read(varName), JsonNode.class);
        } catch (InvalidPathException ex){
          systemMessage = ex.getMessage();
          log.warn("Xsd variable [{}] path problem: {}", varName, systemMessage);
        }

        if(varValue == null || varValue.isNull()) {
          errors.add(new ClientErrorDescription(Errors2.FIELD_NOT_FILED).setInputValidationContext(rootStringErrorContext(varName, jsonNode)).setSystemMessage(systemMessage).setInputValidationContext(rootStringErrorContext(varName, jsonNode)));
        } else {
          if(!varValue.isTextual()) {
            errors.add(new ClientErrorDescription(Errors2.FIELD_WRONG_TYPE).setInputValidationContext(rootStringErrorContext(varName, jsonNode)).setSystemMessage(systemMessage).setInputValidationContext(rootStringErrorContext(varName, jsonNode)));
          }
        }
        if(errors.isEmpty()) {
          String xml = varValue.asText();
          Schema schema = createSchema(factory, xsd, errors);
          Validator validator = schema.newValidator();
          XsdValidationErrorHandler errorhandler = new XsdValidationErrorHandler();
          try {
            validator.validate(new StreamSource(new StringReader(xml)));
          } catch (SAXException | IOException exception) {
            errors.add(new ClientErrorDescription(Errors2.XSD_VALIDATION_FAILURE)
                .setAdviceMessageArgs(new Object[]{exception.getMessage()})
                .setInputValidationContext(rootStringErrorContext(varName, jsonNode)));
          }
          errors.addAll(errorhandler.getErrors());
        }
      }
    }
    return errors;
  }

  private InputValidationContext rootStringErrorContext (String varName, JsonNode validationTarget){
    PropertyViolation pc = new PropertyViolation();
    pc.setPropertyName(varName);
    pc.setPropertyRootPath("$");
    pc.setConstraintType("string");

    InputValidationContext errorCtx = new InputValidationContext();
    errorCtx.setValidationTarget(validationTarget);
    errorCtx.addError(pc);
    return errorCtx;
  }
  
  private Schema createSchema(SchemaFactory factory, String xsdSchema, List<ClientErrorDescription> errrors) {
    Schema result = null;
    try {
      result = factory.newSchema(new StreamSource(new StringReader(xsdSchema)));
    } catch (Exception ex) {
      errrors.add(new ClientErrorDescription(Errors2.PRIMITIVE_PRECONDITION_ERROR)
          .setAdviceMessageArgs(new Object[]{ex.getMessage()}));
    }
    return result;
  }
  
  private final Decoder base64Decoder = Base64.getDecoder();

  private String decodeBase64(String encoded) {
    return new String(base64Decoder.decode(encoded), StandardCharsets.UTF_8);
  }
  
  
}
