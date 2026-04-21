package ru.mts.ip.workflow.engine.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateWorkflowDefinition.IntInterval;
import ru.mts.ip.workflow.engine.dto.Ref;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@JsonInclude(Include.NON_NULL)
public class ResWorkflowDefinition {

  @Schema(requiredMode = RequiredMode.REQUIRED)
  private UUID id;
  private String type;
  @Schema(requiredMode = RequiredMode.REQUIRED, maxLength = 255, minLength = 1)
  private String name;
  @Schema(requiredMode = RequiredMode.NOT_REQUIRED, maxLength = 4000, minLength = 1)
  private String description;
  @Schema(requiredMode = RequiredMode.REQUIRED, maxLength = 255, minLength = 1)
  private String tenantId;
  @Schema(requiredMode = RequiredMode.REQUIRED)
  private OffsetDateTime createTime;
  @Schema(requiredMode = RequiredMode.REQUIRED, minLength = 0)
  private Integer version;

  private JsonNode compiled;
  private JsonNode details;
  private JsonNode flowEditorConfig;


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResWorkflowExpression {
    private String start;
    private List<ResActivity> activities;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResDefinitionDetails {
    private JsonNode inputValidateSchema;
    private JsonNode outputValidateSchema;
    private ResXsdValidation XsdValidation;
    private List<ResStarter> starters;
    private ResSendToKafkaConfig sendToKafkaConfig;
    private ResAwaitForMessageConfig awaitForMessageConfig;
    private ResXsltTransformConfig xsltTransformConfig;
    private ResRestCallConfig restCallConfig;
    private ResDatabaseCallConfig databaseCallConfig;
    private ResSendToSapConfig sendToSapConfig;
    private ResSendToRabbitmqConfig sendToRabbitmqConfig;
    private ResTransform transformConfig;
    private ResSendToS3Config sendToS3Config;
    private List<String> exposedHttpHeaders;
  }


  @Data
  @RequiredArgsConstructor
  @JsonInclude(Include.NON_NULL)
  public static class ResStarter {
    private List<String> tags;
    private String type;
    private String name;
    private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;
    private ResSapInboundStarter sapInbound;
    private ResKafkaConsumer kafkaConsumer;
    private ResRabbitmqConsumer rabbitmqConsumer;
    private ResMailConsumer mailConsumer;
    private ResSchedulerStarterConfig scheduler;

  }

  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResRabbitmqConsumer {
    private ResRabbitmqConnection connectionDef;
    private String queue;
    private JsonNode payloadValidateSchema;
    private JsonNode outputTemplate;
  }

  @Data
  public static class ResRabbitmqConnection {
    private String userName;
    private String userPass;
    private List<String> addresses;
    private String virtualHost;
  }

  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResKafkaConsumer {
    private ResKafkaConnection connectionDef;
    private String topic;
    private String consumerGroupId;
    private JsonNode payloadValidateSchema;
    private JsonNode headersValidateSchema;
    private JsonNode keyValidateSchema;
    private JsonNode outputTemplate;
  }

  public record ResKafkaConnection(String bootstrapServers, ResKafkaAuth authDef,
      List<String> tags) {
  }

  public record ResKafkaAuth(String type, ResSaslAuth sasl, ResTls tls) {
  }

  public record ResSaslAuth(ResSsl sslDef, String protocol, String mechanism, String tokenUrl, String username, String password) {
  }
  
  public record ResSsl(String trustStoreType, String trustStoreCertificates) {
  }
  
  public record ResTls(String keyStoreKey, String keyStoreCertificates, String trustStoreType, String trustStoreCertificates) {
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResSapInboundStarter {
    private ResRef inboundRef;
    private ResSapInbound inboundDef;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResSapInbound {
    private ResSapConnection connectionDef;
    private ResRef connectionRef;
    private String name;
    private String tenantId;
    private String description;
    private Boolean enabled;
    @JsonIgnore
    private Map<String, JsonNode> props;
    private Ref workflowDefinitionToStartRef;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResSendToRabbitmqConfig {
    private ResRabbitmqConnectionDef connectionDef;
    private String exchange;
    private String routingKey;
    private JsonNode message;
    private Map<String, JsonNode> messageProperties;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResRabbitmqConnectionDef {
    @JsonIgnore
    private String userName;
    @JsonIgnore
    private String userPass;
    private List<String> addresses;
    private String virtualHost;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResSendToSapConfig {
    private ResRef connectionRef;
    private ResSapConnection connectionDef;
    private ResIDoc idoc;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResIDoc {
    private String xml;
    private ResRef xmlRef;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResSapConnection {
    private String name;
    private String tenantId;
    private String description;
    @JsonIgnore
    private Map<String, JsonNode> props;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResRestCallConfig {
    private ResRef restCallTemplateRef;
    private ResActivityRestCallTemplate restCallTemplateDef;
    private List<ResManualHandling> resultHandlers;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResManualHandling {
    private ResHttpPredicate predicate;
    private ResValidation validation;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResValidation {
    private JsonNode jsonSchema;
    private ResXsdValidation xsdValidation;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResHttpPredicate {
    private Integer respCode;
    private Set<Integer> respCodes;
    private IntInterval respCodeInterval;
    private List<ResPathValueValidation> respValueAnyOf;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResPathValueValidation {
    private String jsonPath;
    private Set<ValueNode> values;
    private ResPathValueValidation and;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResDatabaseCallConfig {
    private ResDBCall databaseCallDef;
    private ResRef databaseCallRef;
    private UUID dataSourceId;
    private ResDataSource dataSourceDef;


    @Data
    @JsonInclude(Include.NON_NULL)
    public static class ResDBCall {
      private String type;
      private String sql;
      private String schema;
      private String catalog;
      private String functionName;
      private Map<String, ReqSqlType> inParameters;
      private Map<String, ReqSqlType> outParameters;
    }


    @Data
    @JsonInclude(Include.NON_NULL)
    public static class ResSqlParemeterDescription {
      private String name;
      private ReqSqlType type;
    }
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResDataSource {
    private String url;
    private String className;
    @JsonIgnore
    private String userName;
    @JsonIgnore
    private String userPass;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResVariables {
    private Map<String, JsonNode> vars = new HashMap<>();
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResPrimitiveResultRequirements {
    private JsonNode validationSchema;
    private String validationScript;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResActivityRestCallTemplate {
    private String method;
    private String url;
    private JsonNode bodyTemplate;
    private Map<String, String> headers;
    private String curl;
    private ResAuthDef authDef;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResAuthDef {
    private String type;
    private ResOauth2Details oauth2;
    private ResBasicDetails basic;
    private List<String> tags;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResOauth2Details {
    private String issuerLocation;
    private String clientId;
    private String clientSecret;
    private String grantType;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResBasicDetails {
    private String login;
    private String password;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResWorkflowCall {
    private Map<String, JsonNode> args;
    private ResRef workflowRef;
    private ResShortWorkflowDefinition workflowDef;
    private ResRetryConfig retryConfig;
    @JsonInclude(Include.NON_NULL)
    private ResFailActivityResult failActivityResult;
  }


  @JsonInclude(Include.NON_NULL)
  public record ResRetryConfig(@Schema(type = "string", format = "ISO_8601_duration",
      example = "P365D") Duration initialInterval,
      @Schema(type = "string", format = "ISO_8601_duration",
          example = "P365D") Duration maxInterval,
      Integer maxAttempts,
      Double backoffCoefficient) {
  }


  public record ResFailActivityResult(Set<String> retryStates, Map<String, JsonNode> variables) {
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResXsltTransformConfig {
    @Valid
    private ResRef xsltTemplateRef;
    private String xsltTemplate;
    @Valid
    private ResRef xsltTransformTargetRef;
    private String xsltTransformTarget;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResSendToKafkaConfig {
    private String topic;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResAwaitForMessageConfig {
    private String messageName;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResShortWorkflowDefinition {
    private String type;
    private ResDefinitionDetails details;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResActivity {
    private String id;
    private String description;
    private String type;
    private ResWorkflowCall workflowCall;
    private Map<String, JsonNode> injectData;
    private String transition;
    private Map<String, String> outputFilter;
    private List<ResDataCondition> dataConditions;
    private ResDataCondition defaultCondition;
    private List<String> branches;
    private String completionType;
    private String timerDuration;
    private Boolean scoped;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResDataCondition {
    private String id;
    private String condition;
    private String conditionDescription;
    private String successFlowDescription;
    private String transition;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResAction {
    @Schema(requiredMode = RequiredMode.NOT_REQUIRED, maxLength = 1000)
    private String businnesKey;
    private Map<String, JsonNode> args;
    @Schema(requiredMode = RequiredMode.REQUIRED)
    private ResRef workflowRef;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResTransform {
    private String type;
    private Map<String, JsonNode> target;
  }


  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResRestCallTemplate {
    public String method;
    public String url;
    public JsonNode bodyTemplate;
    public Map<String, String> headers;
    public String curl;
  }

  @SuperBuilder()
  @Getter
  @JsonInclude(Include.NON_NULL)
  public static class ResRefWithDeprecated extends ResRef {
    private Set<UUID> deprecatedIds;
  }

  @JsonInclude(Include.NON_NULL)
  public record ResSendToS3Config(ResS3Connection connectionDef, ResS3File s3File, String bucket,
      String region) {

    @JsonInclude(Include.NON_NULL)
    public record ResS3File(String filePath, String contentType) {
    }


    @JsonInclude(Include.NON_NULL)
    public record ResS3Connection(String endpoint, ResS3Auth authDef) {
    }


    @JsonInclude(Include.NON_NULL)
    public record ResS3Auth(String type) {
    }
  }

  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResMailConsumer {
    private ResMailConnection connectionDef;
    private ResMailFilter mailFilter;
    private ResMailPollConfig pollConfig;


    @JsonInclude(Include.NON_NULL)
    public record ResMailConnection(
        String protocol, String host, Integer port, ResMailAuth mailAuth) {
    }

    @JsonInclude(Include.NON_NULL)
    public record ResMailAuth (String username, String password, ResMailCertificate certificate){
    }

    @Data
    public static final class ResMailCertificate {
      private String trustStoreType;
      private String trustStoreCertificates;
    }

    @JsonInclude(Include.NON_NULL)
    public record ResMailFilter(
        List<String> senders,
        List<String> subjects,
        OffsetDateTime startMailDateTime) {
    }
  }

  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResSchedulerStarterConfig {
    private String type;
    private ResCron cron;
    private ResSimpleDuration simple;
  }

  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResCron {
    private String dayOfWeek;
    private String month;
    private String dayOfMonth;
    private String hour;
    private String minute;
  }

  @Data
  public static class ResSimpleDuration {
    private String duration;
  }

  public record ResMailPollConfig(long pollDelaySeconds, int maxFetchSize) {
  }
}
