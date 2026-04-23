package ru.mts.ip.workflow.engine.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class XsdValidation {
  
  private List<XsdImport> imports = new ArrayList<>();
  private List<VariableToValidate> variablesToValidate = new ArrayList<>();
  
  @Data
  public static class XsdImport {
    private String xsdFileName;
    private String base64FileContent;
  }

  @Data
  public static class VariableToValidate {
    private String variableName;
    private String xsdSchemaBase64Content;
  }
  
}
