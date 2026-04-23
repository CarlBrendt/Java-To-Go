package ru.mts.ip.workflow.engine.controller.dto;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Data;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateExecutableWorkflowDefinition.ReqAwaitForMessageConfig;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateExecutableWorkflowDefinition.ReqDatabaseCallConfig;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateExecutableWorkflowDefinition.ReqRef;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateExecutableWorkflowDefinition.ReqRestCallConfig;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateExecutableWorkflowDefinition.ReqSendToKafkaConfig;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateExecutableWorkflowDefinition.ReqSendToRabbitmqConfig;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateExecutableWorkflowDefinition.ReqSendToS3Config;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateExecutableWorkflowDefinition.ReqSendToSapConfig;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateExecutableWorkflowDefinition.ReqTransformConfig;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateExecutableWorkflowDefinition.ReqXsltTransformConfig;

@Data
@JsonInclude(Include.NON_NULL)
public class ReqWorkflowExpressionForValidate {
  
  private String start;
  private List<ReqActivity> activities;
  private Map<String, ReqExecutionConfig> configurations;
  private Map<String, JsonNode> args;

  @Data
  public static class ReqExecutionConfig{
    private Map<String, JsonNode> args;
  }
  
  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ReqActivity {
    
    @Schema(requiredMode = RequiredMode.REQUIRED)
    private String id;
    private String description;
    @Schema(allowableValues = {
      Const.ActivityType.INJECT, 
      Const.ActivityType.PARALLEL, 
      Const.ActivityType.SWITCH, 
      Const.ActivityType.TIMER, 
      Const.ActivityType.WORKFLOW_CALL, 
    })
    private String type;
    private ReqWorkflowCall workflowCall;
    private Map<String, JsonNode> injectData;
    private String transition;
    private Map<String, String> outputFilter;
    private List<ReqDataCondition> dataConditions;
    private ReqDataCondition defaultCondition;
    private List<String> branches;
    private String completionType;
    private String timerDuration;
    private List<String> tags;
    private Boolean scoped;
  }
  
  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ReqDataCondition {
    private String id;
    private String condition;
    private String conditionDescription;
    private String successFlowDescription;
    private String transition;
  }
  
  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ReqWorkflowCall {
    private Map<String, JsonNode> args;
    private ReqRef workflowRef;
    private ReqShortWorkflowDefinition workflowDef;
    private ReqRetryConfig retryConfig;
    private ReqFailActivityResult failActivityResult;
  }

  public record ReqRetryConfig
      (
          @Schema(type = "string", format = "ISO_8601_duration", example = "PT1S")
          Duration initialInterval,
          @Schema(type = "string", format = "ISO_8601_duration", example = "PT100S")
          Duration maxInterval,
          Integer maxAttempts,
          Double backoffCoefficient
      ){}


  public record ReqFailActivityResult(
      @Schema(allowableValues = {
          Const.RetryActivityState.RETRY_STATE_MAXIMUM_ATTEMPTS_REACHED
      })
      Set<String> retryStates,
      Map<String, JsonNode> variables) {
  }

  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ReqShortWorkflowDefinition {
    @Schema(allowableValues = {
      Const.WorkflowType.AWAIT_FOR_MESSAGE, 
      Const.WorkflowType.DB_CALL, 
      Const.WorkflowType.REST_CALL, 
      Const.WorkflowType.SEND_TO_RABBITMQ, 
      Const.WorkflowType.SEND_TO_SAP, 
      Const.WorkflowType.TRANSFORM, 
      Const.WorkflowType.XSLT_TRANSFORM, 
      Const.WorkflowType.SEND_TO_S3,
    })
    private String type;
    private ReqWorkflowDefinitionDetails details;
  }

  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ReqWorkflowDefinitionDetails {
    private JsonNode inputValidateSchema;
    private JsonNode outputValidateSchema;
    private ReqXsdValidation xsdValidation;
    private ReqSendToKafkaConfig sendToKafkaConfig;
    private ReqSendToS3Config sendToS3Config;
    private ReqWorkflowAccessList initialAppendAccessConfigCommand;
    private ReqAwaitForMessageConfig awaitForMessageConfig;
    private ReqXsltTransformConfig xsltTransformConfig;
    private ReqRestCallConfig restCallConfig;
    private ReqDatabaseCallConfig databaseCallConfig;
    private ReqSendToSapConfig sendToSapConfig;
    private ReqSendToRabbitmqConfig sendToRabbitmqConfig;
    private ReqTransformConfig transformConfig;
    private Map<String, String> secrets;
    private List<ReqStarter> starters;
    private List<String> exposedHttpHeaders;
    private ObjectNode intpApi;
  }

  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ReqDefinitionDetails {
    private JsonNode inputValidateSchema;
    private JsonNode outputValidateSchema;
    private ReqXsdValidation outputXsdValidation;
    private Map<String, String> secrets;
    private List<ReqStarter> starters;
  }

}
