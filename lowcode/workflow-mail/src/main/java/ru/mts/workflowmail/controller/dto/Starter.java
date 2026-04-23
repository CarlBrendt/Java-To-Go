package ru.mts.workflowmail.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.workflowmail.service.dto.MailFilter;
import ru.mts.workflowmail.service.Const;
import ru.mts.workflowmail.service.dto.MailConnection;
import ru.mts.workflowmail.service.dto.MailPollConfig;

import java.util.UUID;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Starter {
  private UUID id;
  private String name;
  private final String type = Const.StarterType.MAIL_CONSUMER;
  private String tenantId;
  private String description;
  private UUID workflowDefinitionToStartId;
  private JsonNode workflowInputValidateSchema;
  private MailStarterDetails mailConsumer;


  @Data
  @Accessors(chain = true)
  public static class MailStarterDetails {
    private MailConnection connectionDef;
    private MailFilter mailFilter;
    private MailPollConfig pollConfig;
    private JsonNode outputTemplate;
  }

}
