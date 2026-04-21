package ru.mts.ip.workflow.engine.controller.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ReqXsdValidation {

  private List<ReqXsdImport> imports = new ArrayList<>();
  private List<ReqVariableToValidate> variablesToValidate = new ArrayList<>();

  public ReqXsdValidation copy() {
    ReqXsdValidation copy = new ReqXsdValidation();

    if (this.imports != null) {
      copy.imports = this.imports.stream()
          .map(ReqXsdImport::copy)
          .toList();
    }

    if (this.variablesToValidate != null) {
      copy.variablesToValidate = this.variablesToValidate.stream()
          .map(ReqVariableToValidate::copy)
          .toList();
    }

    return copy;
  }
  
  @Data
  public static class ReqXsdImport {
    private String xsdFileName;
    private String base64FileContent;

    public ReqXsdImport copy() {
      ReqXsdImport copy = new ReqXsdImport();
      copy.xsdFileName = this.xsdFileName;
      copy.base64FileContent = this.base64FileContent;
      return copy;
    }
  }

  @Data
  public static class ReqVariableToValidate {
    private String variableName;
    private String xsdSchemaBase64Content;

    public ReqVariableToValidate copy() {
      ReqVariableToValidate copy = new ReqVariableToValidate();
      copy.variableName = this.variableName;
      copy.xsdSchemaBase64Content = this.xsdSchemaBase64Content;
      return copy;
    }
  }
}
