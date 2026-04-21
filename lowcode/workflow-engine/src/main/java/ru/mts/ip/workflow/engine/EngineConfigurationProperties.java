package ru.mts.ip.workflow.engine;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;

@Data
@Component
@Accessors(chain = true)
@Validated
@ConfigurationProperties(prefix = "app")
public class EngineConfigurationProperties {

  private WorkflowExecutionDefaultConfig workflowExecutionConfig = new WorkflowExecutionDefaultConfig();
  
  private String workflowDslQueueName;
  private String activityFindWorkflowDefinitionQueueName;
  private String activityTransformQueueName;
  private String activityDbCallQueueName;
  private String activityRestCallQueueName;
  private String activitySendToKafkaQueueName;
  private String activitySendToS3QueueName;
  private String activitySendToRabbitmqQueueName;
  private String activitySendToSapQueueName;
  private String activityXsltTransformQueueName;
  private String activityResolveExternalPropertiesQueueName;
  @NotNull
  private String workflowWikiErrorsUrlPattern;
  private boolean securityEnabled;
  private boolean securityClientCredentialsEnabled;
  private String opentelemetryTraceExporterEndpoint;
  private String opentelemetryServiceName;
  private String opentelemetryServiceVersion;
  private String vaultAccessToken;
  private String vaultEndpoint;
  private String vaultRoleId;
  private String vaultSecretId;
  private String vaultAppRolePath;
  private String startUrl;
  private long maxVariableSizeBytes;
  private String auth2ClientId;
  private long instanceSizeLimitBytes;
  private int maxWorkflowThreadCount;
  private int workflowCacheSize;
  private int maxConcurrentActivityExecutionSize;
  private int maxConcurrentWorkflowTaskExecutionSize;
  private int maxWorkerActivitiesPerSecond;
  private int maxTaskQueueActivitiesPerSecond;
  private int syncStartTimeoutLimitSeconds;
  private int syncStartTimeoutDefaultSeconds;
  private Set<String> contextPropagationKeys;
  private boolean localProxyEnabled;

  @Data
  @Accessors(chain = true)
  public static class WorkflowExecutionDefaultConfig {
    private int defaultExecutionTimeoutSeconds;
  }

}
