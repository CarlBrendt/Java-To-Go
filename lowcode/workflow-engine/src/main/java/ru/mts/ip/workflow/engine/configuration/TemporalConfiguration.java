package ru.mts.ip.workflow.engine.configuration;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.common.context.ContextPropagator;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.spring.boot.TemporalOptionsCustomizer;
import io.temporal.worker.WorkerFactoryOptions;
import io.temporal.worker.WorkerOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.mts.ip.workflow.engine.EngineConfigurationProperties;
import ru.mts.ip.workflow.engine.temporal.MdcContextPropagator;

import java.util.List;

@Configuration
public class TemporalConfiguration {

  @Bean
  TemporalOptionsCustomizer<WorkerFactoryOptions.Builder> temporalWorkerFactoryCustomizer(EngineConfigurationProperties props) {
    return b -> 
      b.setMaxWorkflowThreadCount(props.getMaxWorkflowThreadCount())
       .setWorkflowCacheSize(props.getWorkflowCacheSize())

    ;
  }

  @Bean
  TemporalOptionsCustomizer<WorkerOptions.Builder> temporalWorkerCustomizer(EngineConfigurationProperties props) {
    return b -> 
      b.setMaxWorkerActivitiesPerSecond(props.getMaxWorkerActivitiesPerSecond())
       .setMaxTaskQueueActivitiesPerSecond(props.getMaxTaskQueueActivitiesPerSecond())
       .setMaxConcurrentActivityExecutionSize(props.getMaxConcurrentActivityExecutionSize())
       .setMaxConcurrentWorkflowTaskExecutionSize(props.getMaxConcurrentWorkflowTaskExecutionSize())
    ;
  }

  @Bean
  public ContextPropagator mdcContextPropagator(EngineConfigurationProperties props) {
    return new MdcContextPropagator(props.getContextPropagationKeys());
  }
  
}
