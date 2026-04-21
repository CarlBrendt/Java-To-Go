package ru.mts.ip.workflow.engine.llm;

import java.util.List;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;

@Service
@Profile("local")
@RequiredArgsConstructor
public class OllamaChat implements LLMChat {

  private final OllamaChatModel ollama;
  private final String model = "qwen2.5-coder:7b";
  
  @Override
  public LLMAnswer call(String systemMessage, String userMessage, JsonNode format) {
    var prompt = new Prompt(
      List.of(new SystemMessage(systemMessage), new UserMessage(userMessage)),
      OllamaOptions.builder().numCtx(10000)
      .format(format)
      .temperature(0.2).model(model).build()
    );
    var res = ollama.call(prompt);
    return Utils.parseLLMAnswer(res.getResult().getOutput().getText());
  }

}
