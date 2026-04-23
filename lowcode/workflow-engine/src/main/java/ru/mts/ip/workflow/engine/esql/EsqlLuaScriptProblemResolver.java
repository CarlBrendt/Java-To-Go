package ru.mts.ip.workflow.engine.esql;

import java.util.List;
import org.springframework.stereotype.Component;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.esql.EsqlService.SourceFile;
import ru.mts.ip.workflow.engine.esql.temporal.EsqlActivity.ScriptExecutionResult;
import ru.mts.ip.workflow.engine.exception.ErrorDescription;
import ru.mts.ip.workflow.engine.llm.LLMChat;
import ru.mts.ip.workflow.engine.llm.Utils;

@Component
@RequiredArgsConstructor
public class EsqlLuaScriptProblemResolver {

  
  private final String systemPrompt = """
  Ты эксперт lua 5.2
  Твоя задача исправить ошибки в скрипте lua.
  Не спеши с ответом. Подумай хорошенько!
  
  В твоем распоряжении будет: 
  1. Описание стандартных методов и библиотек lua доступных в глобальном контексте.
  2. Скрипт lua
  3. Текст ошибки.
  4. Контекст исполнения.
  
  ```Доспуные стандартные библиотеки:
  bit32
  table
  string
  coroutine
  math
  ```
  ```Доступные методы в глобальном контексте:
  _utils = {
    os = {
      currentTimeMillis = function() //возвращает текущее системное время 
    },
    escapeXml = function(xml),// возвращает эскейпнутый xml
    randomUUID = function(), // возвращает строку сгенерированного UUID type 4
    createXmlHelper = function() // возвращает xml builder
      return {
        parseDocument = function(xml) // создает XmlDocument на основе текста
          return {
            // методы XmlDocument:
            createElementNS = function(namespaceURI, qualifiedName) // создает XmlNode c неймспейсом
              return { // методы XmlNode:
                appendChild = function (child)
                  return self
                end
              }
            end
            createElement = function(tagName) // создает XmlNode
            createTextNode = function(tagName) // создает текстовую XmlNode
            createCDATASection = function(text) // создает CDATA XmlNode
            appendChild = function(child) // Adds the node to the end of the list of childrenof this node
              return self
            end
            toString = function() return node xml text with omitXmlDeclaration = no
            xpath = function() // возвращает Xpath
              return {
                // методы Xpath:
                find = function(expression) возвращает NodeList
                  return {
                    //методы NodeList:
                    toString = function() // return text node value
                    toStringXml = function() // return node xml text with omitXmlDeclaration = yes
                  }
                end
              }
            end
          }
        end
      }
    end
  }
  ```
  Очень Важно!: Твой ответ должен содержать только исправденный луа скрипт, без пояснений!
  """;
  
  private final String userPromptTemplate = """
  ```Скрипт lua:
  %s
  ```
  ```Текст ошибки:
  %s
  ```
  ```Контекст исполнения:
  %s
  ```
  """;
  
  private final LLMChat chat;
  
  @SneakyThrows
  public SourceFile fixLuaScript(LuaScriptWithErrors script) {
    var errors = script.getExecutionResult().getErrors();
    var scriptContext = errors.get(0).getScriptContext();
    var luaContext = Utils.jsonNodeToLuaValue(scriptContext.getVariableContext());
    var userPrompt = userPromptTemplate.formatted(script.getSrc().getContent(), scriptContext.getSystemMessage(), luaContext);
    var chatAnswer = chat.call(systemPrompt, userPrompt, null);
    return new SourceFile().setContent(findScript(chatAnswer.getResult()));
  }
  
  
  @Data
  @Accessors(chain = true)
  public static class LuaScriptWithErrors {
    private SourceFile src;
    private ScriptExecutionResult executionResult;
  }
  
  private String findScript(String llmAnswerText) {
    if (llmAnswerText.contains("```lua")) {
      var arr = llmAnswerText.split("```lua");
      llmAnswerText = arr[1];
      llmAnswerText = llmAnswerText.substring(0, llmAnswerText.length() - 3);
    }
    return llmAnswerText;
  }
  
 
}
