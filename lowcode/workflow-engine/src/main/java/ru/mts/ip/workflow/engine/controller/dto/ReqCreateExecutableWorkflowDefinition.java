package ru.mts.ip.workflow.engine.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateWorkflowDefinition.IntInterval;
import ru.mts.ip.workflow.engine.controller.dto.ReqWorkflowExpressionForValidate.ReqDefinitionDetails;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Valid
public class ReqCreateExecutableWorkflowDefinition {

  private ReqWorkflowExpression compiled;
  private ReqDefinitionDetails details;
  private JsonNode flowEditorConfig;
  
  @Schema(
    allowableValues = {
      Const.ActivityType.INJECT,
      Const.ActivityType.PARALLEL,
      Const.ActivityType.SWITCH,
      Const.ActivityType.TIMER,
      Const.ActivityType.WORKFLOW_CALL,
    }
  )
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
    private Map<String, JsonNode> target;
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
  public static class ReqSendToKafkaConfig {
    private ReqKafkaConnection connectionDef;
    private ReqKafkaMessage message;
    private String topic;
    private String key;
    private Map<String, String> headers;
  }
  
  @Data
  public static class ReqKafkaMessage {
    private JsonNode payload;
  }
  
  @Data
  public static class ReqKafkaConnection {
    private String bootstrapServers;
    private ReqKafkaAuth authDef;
    private List<String> tags;
  }
  
  @Data
  public static class ReqKafkaAuth {
    private String type;
    private ReqSaslAuth sasl;
    private ReqSsl tls;
  }
  
  @Data
  public static class ReqSaslAuth {
    private ReqSsl sslDef;
    private String protocol;
    private String mechanism;
    private String username;
    private String password;
    private String tokenUrl;
  }
  
  @Data
  public static class ReqSsl {
    private String trustStoreType;
    private String trustStoreCertificates;
    private String keyStoreKey;
    private String keyStoreCertificates;
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
  public static class ReqManualHandling {
    private ReqHttpPredicate predicate;
    private ReqValidation validation;
  }

  @Data
  public static class ReqValidation {
    private JsonNode jsonSchema;
  }


  @Data
  public static class ReqHttpPredicate {
    private Integer respCode;
    private Set<Integer> respCodes;
    private IntInterval respCodeInterval;
    private List<ReqPathValueValidation> respValueAnyOf;
  }


  @Data
  public static class ReqPathValueValidation {
    @Schema(requiredMode = RequiredMode.REQUIRED, example = "jp{body.field1}",
        description = "json path for expected response. If you expect response body like {\"field1\":\"value1\"} you can put jsonPath = jp{body.field1} ")
    private String jsonPath;
    @ArraySchema(schema = @Schema(requiredMode = RequiredMode.REQUIRED,
        description = "Excepted values for jsonPath. You can use boolean, String and number values",
        example = "value1"), minItems = 1)
    private Set<ValueNode> values;
    @Schema(implementation = ReqPathValueValidation.class,
        description = "Recursive ReqPathValueValidation object",
        example = "{\"jsonPath\":\"jp{body.field2}\",\"values\":[false],\"and\":null}")
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
  public static class ReqRef {
    @Schema(requiredMode = RequiredMode.NOT_REQUIRED)
    private UUID id;
    @Schema(requiredMode = RequiredMode.NOT_REQUIRED)
    private String name;
    @Schema(requiredMode = RequiredMode.NOT_REQUIRED)
    private String version;
    @Schema(requiredMode = RequiredMode.NOT_REQUIRED)
    private String stand;
    @Schema(requiredMode = RequiredMode.NOT_REQUIRED)
    private String tenantId;
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


  public record ReqSendToS3Config(
      @Schema(requiredMode = RequiredMode.REQUIRED)
      ReqS3Connection connectionDef,
      @Schema(requiredMode = RequiredMode.REQUIRED)
      ReqS3File s3File,
      @Schema(requiredMode = RequiredMode.REQUIRED, maxLength = 63, minLength = 3,
          example = "bucket",
          description = "The name of the S3 bucket. Can contain only lowercase letters, numbers, dots, and hyphens")
      String bucket,
      @Schema(example = "us-east-1")
      String region) {

    public record ReqS3File(
        @Schema(requiredMode = RequiredMode.REQUIRED, example = "path/file.txt")
        String filePath,
        String contentType,
        String content) {
    }


    public record ReqS3Connection(
        @Schema(requiredMode = RequiredMode.REQUIRED)
        String endpoint,
        @Schema(requiredMode = RequiredMode.REQUIRED)
        ReqS3Auth authDef) {
    }


    public record ReqS3Auth(String type,
        ReqAccessKeyAuth accessKeyAuth) {
    }


    public record ReqAccessKeyAuth(
        @Schema(requiredMode = RequiredMode.REQUIRED)
        String accessKey,
        @Schema(requiredMode = RequiredMode.REQUIRED)
        String secretKey) {
    }
  }
}
