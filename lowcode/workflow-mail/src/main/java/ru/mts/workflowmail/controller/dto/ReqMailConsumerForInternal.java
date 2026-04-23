package ru.mts.workflowmail.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class ReqMailConsumerForInternal {
  private JsonNode outputTemplate;
}
