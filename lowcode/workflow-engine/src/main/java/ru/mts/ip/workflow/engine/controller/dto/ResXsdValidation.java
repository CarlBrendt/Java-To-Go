package ru.mts.ip.workflow.engine.controller.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ResXsdValidation {

  private List<ResXsdImport> imports = new ArrayList<>();
  private List<ResVariableToValidate> variablesToValidate = new ArrayList<>();
  
  @Data
  public static class ResXsdImport {
    private String xsdFileName;
    private String base64FileContent;
  }

  @Data
  public static class ResVariableToValidate {
    private String variableName;
    private String xsdSchemaBase64Content;
  }
}
