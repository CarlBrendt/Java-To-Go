package ru.mts.ip.workflow.engine;

import java.lang.management.ManagementFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import lombok.extern.slf4j.Slf4j;

@EnableScheduling
@EnableFeignClients
@SpringBootApplication
@OpenAPIDefinition(
  info = @Info(title = "Workflow engine")
)
@Slf4j
public class Application {
  
  public static void main(String[] args) {
    log.info("ActiveProcessorCount:: {}",  Runtime.getRuntime().availableProcessors());
    log.info("JVM arguments: {}",  ManagementFactory.getRuntimeMXBean().getInputArguments());
    SpringApplication.run(Application.class, args);
  }
  
  
}