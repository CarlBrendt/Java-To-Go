package ru.mts.ip.workflow.engine.esql;

import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.esql.EsqlService.SourceFile;
import ru.mts.ip.workflow.engine.llm.LLMChat;

@Component
@RequiredArgsConstructor
public class LuaTestDataGenerator {
  
  private final String systemPrompt = """
  Ты эксперт разработчик 5.2
  Твоя задача проанализировать lua скрипт и сгенерировать контекст исполнения чтобы протестировать основную часть скрипта.
  В твоем распоряжении будет lua скрипт для анализа.
  На выходе будет json представление этого контекста.
  Важно!: Постарайся сгенерировать полный набор переменных со всеми вложениями.
  
  Простой пример:
  ```Lua скрипт для анализа
  local xml = input.xml
  local getOnlyId = args.getOnlyId
  local xmlHelper = _utils.createXmlHelper()
  local inputDoc = xmlHelper.parseDocument(xml)
  local xpath = inputDoc.xpath()
  
  local function getText(path)
    local node = xpath.find(path)
    if node then
      return node.toString()
    else
      return ""
    end
  end
  if getOnlyId then 
    return getText("/root/data/id")
  else
    return getText("/root/data/user")
  end
  
  ```
  Результат:
  {
    "input": {"xml": "<root><data><id>aef640a4-1546-42bd-bcf1-0b1b4e2fbb4a</id><user>text</user></data></root>"},
    "args": {"getOnlyRequestUID": "false"}
  }
  Важно: Твой ответ должен содержать только json, без пояснений!
  """;

  private final String userPromptTemplate = """
  ```Lua скрипт для анализа
  %s
  ```
  """;
  
  private final LLMChat chat;
  
  @Data
  @Accessors(chain = true)
  public static class LuaTestContext {
    private JsonNode vars;
  }

  private final ObjectMapper OM = new ObjectMapper();
  
  @SneakyThrows
  public LuaTestContext generateTestData(SourceFile file) {
    var chatAnswer = chat.call(systemPrompt, userPromptTemplate.formatted(file.getContent()), null);
    return new LuaTestContext().setVars(OM.readTree(chatAnswer.getResult()));
  }

}
