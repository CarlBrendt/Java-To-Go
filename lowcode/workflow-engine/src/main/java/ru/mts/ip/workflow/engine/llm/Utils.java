package ru.mts.ip.workflow.engine.llm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.esql.EsqlService.SourceFile;
import ru.mts.ip.workflow.engine.llm.LLMChat.LLMAnswer;

public class Utils {

  public static LLMAnswer parseLLMAnswer(String text) {
    String think = null;
    if (text.contains("</think>")) {
      var arr = text.split("</think>");
      think = arr[0];
      text = arr[1];
    }
    if (text.contains("```json")) {
      var arr = text.split("```json");
      text = arr[1];
      text = text.substring(0, text.length() - 3);
    }

    return new LLMAnswer().setThink(think).setResult(text);
  }

  public static List<String> findLabels(String script) {
    List<String> labels = new ArrayList<>();
    Pattern pattern = Pattern.compile("PROPAGATE\\s+TO\\s+LABEL\\s+(?:'([^']*)'|\"([^\"]*)\")",
        Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(script);
    while (matcher.find()) {
      String label = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
      if (label != null && !label.trim().isEmpty()) {
        labels.add(label);
      }
    }
    return labels;
  }
  
  public static String asPlaneText(List<SourceFile> sources) {
    if(sources != null) {
      StringBuilder sb = new StringBuilder();
      sources.forEach(s -> {
        sb.append(s.getContent()).append("\r\n");
      });
      return sb.toString();
    }
    return "";
  }

  public static List<String> findLabels(List<SourceFile> sources) {
    return findLabels(asPlaneText(sources));
  }

  public static String jsonNodeToLuaValue(JsonNode json) {
    if (json == null || json.isNull()) {
      return "nil";
    }

    if (json.isBoolean()) {
      return Boolean.toString(json.asBoolean());
    }

    if (json.isNumber()) {
      // Сохраняем оригинальное представление числа
      return json.asText();
    }

    if (json.isTextual()) {
      return escapeString(json.asText());
    }

    if (json.isArray()) {
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      for (int i = 0; i < json.size(); i++) {
        if (i > 0) {
          sb.append(", ");
        }
        // Индексы в Lua начинаются с 1
        sb.append("[").append(i + 1).append("] = ");
        sb.append(jsonNodeToLuaValue(json.get(i)));
      }
      sb.append("}");
      return sb.toString();
    }

    if (json.isObject()) {
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      Iterator<Map.Entry<String, JsonNode>> fields = json.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        sb.append(field.getKey()).append(" = ");
        sb.append(jsonNodeToLuaValue(field.getValue()));
        if (fields.hasNext()) {
          sb.append(", ");
        }
      }
      sb.append("}");
      return sb.toString();
    }

    return "nil";
  }

  private static String escapeString(String text) {
    return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        .replace("\r", "\\r").replace("\t", "\\t") + "\"";
  }


}
