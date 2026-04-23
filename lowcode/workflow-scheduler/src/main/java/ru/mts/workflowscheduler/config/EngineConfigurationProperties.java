package ru.mts.workflowscheduler.config;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@Accessors(chain = true)
@ConfigurationProperties(prefix = "app")
public class EngineConfigurationProperties {

  private WorkflowExecutionDefaultConfig workflowExecutionConfig = new WorkflowExecutionDefaultConfig();

  private String workflowDslQueueName;
  private String activityFindWorkflowDefinitionQueueName;
  private String activityDbCallQueueName;
  private String activityRestCallQueueName;
  private String activitySendToKafkaQueueName;
  private String activitySendToRabbitmqQueueName;
  private String activitySendToSapQueueName;
  private String activityXsltTransformQueueName;
  private String workflowWikiErrorsUrlPattern;
  private boolean securityEnabled;

  private String opentelemetryTraceExporterEndpoint;
  private String opentelemetryServiceName;
  private String opentelemetryServiceVersion;
  private int schedulerWorkerLimitCount;
  private long maxVariableSizeBytes;
  private long maxProducerPoolSize;


  @Data
  @Accessors(chain = true)
  public static class WorkflowExecutionDefaultConfig {
    private Duration defaultExecutionTimeout;
  }

}
