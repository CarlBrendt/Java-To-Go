package ru.mts.ip.workflow.engine.llm;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

public interface LLMChat {
  LLMAnswer call(String systemMessage, String userMessage, JsonNode format);
  
  @Data
  @Accessors(chain = true)
  public class LLMAnswer {
    private String think;
    private String result;
  }
  
}
