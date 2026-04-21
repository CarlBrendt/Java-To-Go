package ru.mts.ip.workflow.engine.controller.dto;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.Const;

@Data
@Valid
public class ReqCreateWorkflowDefinition {

  private JsonNode compiled;
  private JsonNode details;
  private JsonNode flowEditorConfig;

  private UUID id;
  private String type;
  private String name;
  private String description;
  private String tenantId;
  private String ownerLogin;
  

  @Data
  public static class ReqSendToSapConfig {
    private ReqRef connectionRef;
    private ReqSapConnection connectionDef;
    private ReqIDoc idoc;
  }
  
  @Data
  public static class ReqIDoc {
    private String xml;
    private ReqRef xmlRef;
  }

  @Data
  public static class ReqTransformConfig {
    private String type;
    private ReqCsvConfig csvConfig;
    private Map<String, JsonNode> target;
  }

  @Data
  public static class ReqCsvConfig {
    private String delimiter;
    @Schema(
        implementation = Const.CsvOutputFormat.class
    )
    private String outputFormat;
  }

  @Data
  public static class ReqXsltTransformConfig {
    @Valid
    private ReqRef xsltTemplateRef;
    private String xsltTemplate;
    @Valid
    private ReqRef xsltTransformTargetRef;
    private String xsltTransformTarget;
  }

  @Data
  public static class ReqAwaitForMessageConfig {
    private String messageName;
  }

  @Data
  public static class ReqRestCallConfig {
    private ReqRef restCallTemplateRef;
    private ReqRestCallTemplate restCallTemplateDef;
    private List<String> exposedHeaders;
    private List<ReqManualHandling> resultHandlers;
  }
  
  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ReqManualHandling {
    private ReqHttpPredicate predicate;
    private Validation validation;
  }
  
  @Data
  @Accessors(chain = true)
  @JsonInclude(Include.NON_NULL)
  public static class Validation {
    private JsonNode jsonSchema;
    private ReqXsdValidation xsdValidation;
    private ReqYamlValidation yamlValidation;

    public Validation copy() {
      return new Validation()
          .setJsonSchema(Optional.ofNullable(jsonSchema).map(f -> (JsonNode)f.deepCopy()).orElse(null))
          .setXsdValidation(Optional.ofNullable(xsdValidation).map(x->x.copy()).orElse(null))
          .setYamlValidation(Optional.ofNullable(yamlValidation).map(x->x.copy()).orElse(null));
    }
  }

  @Data
  public static class ReqHttpPredicate{
    private Integer respCode;
    private Set<Integer> respCodes;
    private IntInterval respCodeInterval;
    private List<ReqPathValueValidation> respValueAnyOf;
  }

  @Data
  @Accessors(chain = true)
  @JsonInclude(Include.NON_NULL)
  public static class IntInterval {
    private Integer from;
    private Integer to;

    public IntInterval copy() {
      return new IntInterval()
          .setFrom(from)
          .setTo(to);
    }
  }
  

  @Data
  public static class ReqPathValueValidation {
    private String jsonPath;
    private Set<ValueNode> values;
    private ReqPathValueValidation and;
  }

  @Data
  public static class ReqRestCallTemplate {
    public String method;
    public String url;
    public JsonNode bodyTemplate;
    public Map<String, String> headers;
    public String curl;
    public ReqAuthDef authDef;
  }

  @Data
  public static class ReqAuthDef{
    private String type;
    private ReqOauth2Details oauth2;
    private ReqBasicDetails basic;
    private List<String> tags;
  }
  
  @Data
  public static class ReqBasicDetails{
    private String login;
    private String password;
  }

  @Data
  public static class ReqOauth2Details{
    private String issuerLocation;
    private String clientId;
    private String clientSecret;
//    @AnyOf("client_credentials")
    private String grantType;
  }

  @Data
  public static class ReqSendToRabbitmqConfig {
    private ReqRabbitmqConnectionDef connectionDef;
    private String exchange;
    private String routingKey;
    private JsonNode message;
    private Map<String, JsonNode> messageProperties;
  }

  @Data
  public static class ReqRabbitmqConnectionDef {
    private String userName;
    private String userPass;
    private List<String> addresses;
    private String virtualHost;
  }

  @Data
  public static class ReqSapConnection {
    private Map<String, JsonNode> props;
  }

  @Data
  public static class ReqDatabaseCallConfig {
    private ReqDBCall databaseCallDef;
    private ReqRef databaseCallRef;
    private UUID dataSourceId;
    private ReqDataSource dataSourceDef;
    
    @Data
    public static class ReqDBCall{
      private String type;
      private String sql;
      private String schema;
      private String catalog;
      private String functionName;
      private Map<String, ReqSqlType> inParameters;
      private Map<String, ReqSqlType> outParameters;
    }

    @Data
    public static class ReqSqlParemeterDescription{
      private String name;
      private ReqSqlType type;
    }
    
  }
  
  @Data
  public static class ReqDataSource {
    private String url;
    private String className;
    private String userName;
    private String userPass;
  }

  @Data
  @Accessors(chain = true)
  public static class ReqDataCondition {
    @Schema(requiredMode = RequiredMode.REQUIRED, maxLength = 255, minLength = 1)
    private String id;
    @Schema(requiredMode = RequiredMode.REQUIRED, maxLength = 400)
    private String condition;
    @Schema(requiredMode = RequiredMode.NOT_REQUIRED, maxLength = 400)
    private String conditionDescription;
    @Schema(requiredMode = RequiredMode.NOT_REQUIRED, maxLength = 400)
    private String successFlowDescription;
    @Schema(requiredMode = RequiredMode.NOT_REQUIRED, maxLength = 255)
    private String transition;
  }

}
