package ru.mts.workflowscheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import ru.mts.workflowscheduler.service.Const;
import ru.mts.workflowscheduler.service.VariablesJsonSchema;

@EnableAsync
@EnableFeignClients
@SpringBootApplication
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Bean
  @SneakyThrows
  public VariablesJsonSchema schedulerDefaultValidationSchema(ObjectMapper om) {
    return om.readValue(Const.StarterJsonSchema.SCHEDULER_JSON, VariablesJsonSchema.class);
  }
}
