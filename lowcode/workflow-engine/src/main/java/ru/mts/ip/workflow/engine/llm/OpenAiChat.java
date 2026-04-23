package ru.mts.ip.workflow.engine.llm;

import java.util.List;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;

@Service
@Profile("default")
@RequiredArgsConstructor
public class OpenAiChat implements LLMChat {

  private final OpenAiChatModel chatModel;
  private final String model = "cotype-pro-2_128k_030325";
  
  @Override
  public LLMAnswer call(String systemMessage, String userMessage, JsonNode format) {
    var prompt = new Prompt(
      List.of(new SystemMessage(systemMessage), new UserMessage(userMessage)),
      OpenAiChatOptions.builder().maxCompletionTokens(10000).temperature(0.2).model(model).build()
    );
    var res = chatModel.call(prompt);
    return Utils.parseLLMAnswer(res.getResult().getOutput().getText());
  }

}
