package ru.mts.ip.workflow.engine.controller.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@RequiredArgsConstructor
public class ResDefinitionInspection {
  
  private final List<InspectionApiInfo> intpApis;
  private final List<InspectionImsInfo> imsList;
  
  @Data
  public static class InspectionApiInfo {
    private String versionId;
    private String stand;
    private String ims;
    private Auth auth;
  }

  @Data
  @Accessors(chain = true)
  public static class InspectionImsInfo {
    private String type;
    private String ims;
    private String name;
  }

  @Data
  @Accessors(chain = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Auth {
    private String type;
    private String clientId;
    private String clientIdPath;
    private String clientIdField;
    private String secret;
    private String secretPath;
    private String secretField;
    private String serviceAccount;
    private String serviceAccountPath;
    private String serviceAccountField;
  }
  
}
