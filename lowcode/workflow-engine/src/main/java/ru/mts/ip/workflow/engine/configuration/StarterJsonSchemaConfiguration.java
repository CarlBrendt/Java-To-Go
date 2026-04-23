package ru.mts.ip.workflow.engine.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.json.VariablesJsonSchema;

@Configuration
public class StarterJsonSchemaConfiguration {

  @Bean
  @SneakyThrows
  public VariablesJsonSchema kafkaDefaultValidationSchema(ObjectMapper om) {
    return om.readValue(Const.StarterJsonSchema.KAFKA_CONSUMER, VariablesJsonSchema.class);
  }

  @Bean
  @SneakyThrows
  public VariablesJsonSchema rabbitmqDefaultValidationSchema(ObjectMapper om) {
    return om.readValue(Const.StarterJsonSchema.RABBIT_MQ_CONSUMER, VariablesJsonSchema.class);
  }

  @Bean
  @SneakyThrows
  public VariablesJsonSchema sapDefaultValidationSchema(ObjectMapper om) {
    return om.readValue(Const.StarterJsonSchema.SAP_INBOUND, VariablesJsonSchema.class);
  }

  @Bean
  @SneakyThrows
  public VariablesJsonSchema mailDefaultValidationSchema(ObjectMapper om) {
    return om.readValue(Const.StarterJsonSchema.MAIL_CONSUMER, VariablesJsonSchema.class);
  }

  @Bean
  @SneakyThrows
  public VariablesJsonSchema ibmmqDefaultValidationSchema(ObjectMapper om) {
    return om.readValue(Const.StarterJsonSchema.IBMMQ_CONSUMER, VariablesJsonSchema.class);
  }
}
