package ru.mts.ip.workflow.engine.esql;

import java.util.List;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import ru.mts.ip.workflow.engine.esql.EsqlService.SourceFile;
import ru.mts.ip.workflow.engine.esql.temporal.EsqlActivity.CompileTarget;
import ru.mts.ip.workflow.engine.llm.LLMChat;

@Component
@RequiredArgsConstructor
public class EsqlComputeModuleToLuaCompiler {

  
  private final String systemPrompt = """
  Ты эксперт IBM ESQL и lua 5.2
  Твоя задача переписать ESQL Compute module на lua скрипт.
  
  В твоем распоряжении будет: 
  1. ESQL cкрипт Compute module.
  2. Файлы с содержимым внешних модулей если есть.
  3. Описание уже реализованных методов и библиотек lua.

  Lua скрипт должен будет вернуть таблицу следующего формата:
  { 
    success = false, // Main function return value
    propagateLabels = {label1 = "", label2 = ""}, // label propagate values if exists
    OutputRoot = "", // final output data
  }
  
  Важные ограничения:
  1. Реализованные методы не предназначенны для привема self. Поэтому передвать self в реализованные методы запрещенно в явном и не явном виде!
  2. Для созания узлов xml используй document а не xmlHelper!
  3. Твой ответ должен содержать только lua скрипт, без пояснений!
  4. Проверяй входящие переменные перед парсингом, не передавать в xmlHelper пустую строку!
  
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
  Пример: 
  ```ESQL cкрипт Compute module
  DECLARE bdr_ns NAMESPACE 'http://global.dellin.ru/bus/data-receiving'; 
  DECLARE publishing_ns NAMESPACE 'http://global.dellin.ru/bus/common_DataPublishing';
  
  DECLARE docDisableSklad     EXTERNAL BOOLEAN FALSE;
  
  CREATE COMPUTE MODULE TransformDocumentsFlow_MapDocuments
      CREATE FUNCTION Main() RETURNS BOOLEAN
      BEGIN       
          
          DECLARE publishDoc REFERENCE TO InputRoot.XMLNSC.publishing_ns:publishDocument;
          
          CALL CopyEntireMessage();
          SET OutputRoot.XMLNSC = NULL;
                  
          SET OutputRoot.XMLNSC.bdr_ns:receiveData.bdr_ns:out = bdr_ns;
      
          SET OutputRoot.XMLNSC.bdr_ns:receiveData.bdr_ns:iBusData.bdr_ns:requestUID = FIELDVALUE(publishDoc.publishing_ns:in.ns:requestUID);
          
          DECLARE iBusData REFERENCE TO OutputRoot.XMLNSC.bdr_ns:receiveData.bdr_ns:iBusData;
          
          SET iBusData.bdr_ns:messageKind                 =   'PublishedData'; 
          SET iBusData.bdr_ns:sender                      =   FIELDVALUE(publishDoc.publishing_ns:in.ns:sender);
          SET iBusData.bdr_ns:senderDateTime              =   FIELDVALUE(publishDoc.publishing_ns:in.ns:senderDateTime);
          SET iBusData.bdr_ns:currentSenderApplication    =   FIELDVALUE(publishDoc.publishing_ns:in.ns:currentSenderApplication);
          
          IF (NOT docDisableSklad) THEN
              PROPAGATE TO LABEL 'sklad'; 
          END IF;
                          
          RETURN TRUE;
      END;
  
      CREATE PROCEDURE CopyEntireMessage() BEGIN
          SET OutputRoot = InputRoot;
      END;
  END MODULE;
  ```
  Ответ:
  ```lua
  propagateLabels = {}
  docDisableSklad = docDisableSklad or false
  result = {success = false, propagateLabels = propagateLabels}
  if InputRoot and InputRoot.XMLNSC then
    bdr_ns = 'http://global.dellin.ru/bus/data-receiving'
    inputXml = InputRoot.XMLNSC
    xmlHelper = _utils.createXmlHelper()
    inputDoc = xmlHelper.parseDocument(inputXml)
    xpath = inputDoc.xpath()
    requestUID = xpath.find("/publishDocument/in/requestUID")
    sender = xpath.find("/publishDocument/in/sender")
    senderDateTime = xpath.find("/publishDocument/in/senderDateTime")
    currentSenderApplication = xpath.find("/publishDocument/in/currentSenderApplication")

    -- Create out Document
    outputDoc = xmlHelper.createDocument()
    receiveDataElem = outputDoc.createElementNS(bdr_ns, "bdr_ns:receiveData")
    outputDoc.appendChild(receiveDataElem)
    
    -- Create out Element
    out = outputDoc.createElement("bdr_ns:out")
    out.appendChild(outputDoc.createTextNode(bdr_ns))
    receiveDataElem.appendChild(out)
    
    -- Create iBusData element
    iBusDataElem = outputDoc.createElement("bdr_ns:iBusData")
    receiveDataElem.appendChild(iBusDataElem)
    
    -- Add fields to iBusData
    local function addField(parent, tag, value)
        field = outputDoc.createElement(tag)
        field.appendChild(outputDoc.createTextNode(value or ""))
        parent.appendChild(field)
    end
    
    addField(iBusDataElem, "bdr_ns:requestUID", requestUID.toString())
    addField(iBusDataElem, "bdr_ns:messageKind", "PublishedData")
    addField(iBusDataElem, "bdr_ns:sender", sender.toString())
    addField(iBusDataElem, "bdr_ns:senderDateTime", senderDateTime.toString())
    addField(iBusDataElem, "bdr_ns:currentSenderApplication", currentSenderApplication.toString())
    
    outputXml = outputDoc.toString()
    result['success'] = true
    result['OutputRoot'] = outputXml
    
    if not docDisableSklad then
      table.insert(propagateLabels, {sklad = outputXml})
    end

  end
  return result  
  ```
  Не пиши ответ сразу! Подумай как следует!
  """;
  
  private final String userPromptTemplate = """
  ```ESQL cкрипт Compute module
  %s
  ```
  Внешние модули:
  %s
  """;
  
  private final LLMChat chat;
  
  @SneakyThrows
  public SourceFile compileToLua(CompileTarget data) {
    var file = findComputeModuleScript(data.getSources());
    var chatAnswer = chat.call(systemPrompt, userPromptTemplate.formatted(file.getContent(), findModules()), null);
    return new SourceFile().setContent(findScript(chatAnswer.getResult()));
  }
  
  private String findModules() {
    return "[]";
  }
  
  private SourceFile findComputeModuleScript(List<SourceFile> esqlSources) {
    return  esqlSources.get(0);
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
