package ru.mts.workflowmail;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import ru.mts.workflowmail.service.Const;
import ru.mts.workflowmail.service.VariablesJsonSchema;

@EnableAsync
@EnableFeignClients
@SpringBootApplication
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Bean
  @SneakyThrows
  public VariablesJsonSchema mailDefaultValidationSchema(ObjectMapper om) {
    return om.readValue(Const.StarterJsonSchema.MAIL_JSON, VariablesJsonSchema.class);
  }
}
