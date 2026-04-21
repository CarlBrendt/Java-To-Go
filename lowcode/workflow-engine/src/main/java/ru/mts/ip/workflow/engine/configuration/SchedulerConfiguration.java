package ru.mts.ip.workflow.engine.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ErrorHandler;

@EnableScheduling
@Configuration
@Slf4j
public class SchedulerConfiguration {

  @Bean
  public TaskScheduler taskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(3);
    scheduler.setErrorHandler(taskErrorHandler());
    scheduler.initialize();
    return scheduler;
  }

  @Bean
  public ErrorHandler taskErrorHandler() {
    return ex -> log.error("Ошибка в scheduled task: ", ex);
  }
}
