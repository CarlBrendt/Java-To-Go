package ru.mts.ip.workflow.engine.lang.plant;

import java.nio.channels.IllegalSelectorException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.Const.Errors2;
import ru.mts.ip.workflow.engine.lang.plant.WorkflowExpression.Activity;

public class PlantUtils {

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .setSerializationInclusion(Include.NON_NULL);

  public static <T> T readInstruction(String text, Class<T> clazz) {
    try {
      return OBJECT_MAPPER.readValue(text, clazz);
    } catch (JsonProcessingException e) {
      throw new PlantUmlSyntaxError(e.getMessage());
    }
  }

  public static <T> T readInstruction(JsonNode node, Class<T> clazz) {
    try {
      return OBJECT_MAPPER.treeToValue(node, clazz);
    } catch (JsonProcessingException e) {
      throw new PlantUmlSyntaxError(e.getMessage());
    }
  }

  public static <T> T readInstruction(String text, TypeReference<T> valueTypeRef) {
    try {
      return OBJECT_MAPPER.readValue(text, valueTypeRef);
    } catch (JsonProcessingException e) {
      throw new PlantUmlSyntaxError(e.getMessage());
    }
  }

  public static String writeValueAsString(Object obj) {
    try {
      return OBJECT_MAPPER.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new PlantUmlSyntaxError(e.getMessage());
    }
  }

  public static JsonNode writeValueAsNode(Object obj) {
    return OBJECT_MAPPER.valueToTree(obj);
  }

  public static String prettyWriteValueAsString(Object obj) {
    try {
      return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new PlantUmlSyntaxError(e.getMessage());
    }
  }

  public static void linkArgs(List<PlantInstruction> args) {
    for (int i = 0; i < args.size(); i++) {
      PlantInstruction arg = args.get(i);
      PlantInstruction next = null;
      if (i + 1 < args.size()) {
        next = args.get(i + 1);
      }
      arg.setNext(next);
    }
  }


  public static List<String> findAllTransitions(Activity s, Pouch pouch, List<String> seq) {
    if (s == null) {
      return new ArrayList<>();
    }
    List<String> res = new ArrayList<>(seq);
    Set<String> posibles = s.findPosibleTransitions();
    for (String p : posibles) {
      if (!res.contains(p)) {
        res.add(p);
        Activity state = pouch.get(p);
        findAllTransitions(state, pouch, res).stream().filter(v -> !res.contains(v))
            .forEach(res::add);
      }
    }
    return res;
  }

  public static List<String> seq(Activity s, Pouch pouch, List<String> seq) {
    if (s == null) {
      return new ArrayList<>();
    }
    List<String> res = new ArrayList<>(seq);
    String next = s.getTransition();
    if ("switch".equals(s.getType())) {
      next = findExit(s, pouch);
    }
    if (!res.contains(next)) {
      res.add(next);
      Activity state = pouch.get(next);
      seq(state, pouch, res).stream().filter(v -> !res.contains(v)).forEach(res::add);
    }
    return res;
  }

  private static String findIext(Set<String> roots, Pouch p) {

    List<List<String>> posibles = new ArrayList<>();
    for (String root : roots) {
      posibles.add(findAllTransitions(p.get(root), p, new ArrayList<>()));
    }

    List<List<String>> sortes = posibles.stream().sorted((l, v) -> v.size() - l.size()).toList();
    List<String> f = sortes.stream().findFirst().orElseThrow();


    for (String c : f) {
      boolean all = true;
      for (List<String> seq : posibles) {
        all &= seq.contains(c);
      }
      if (all) {
        return c;
      }
    }
    throw new IllegalSelectorException();
  }

  public static boolean isLoop(String id, Pouch p) {
    Activity s = p.get(id);
    return findAllTransitions(s, p, new ArrayList<>()).contains(s.getId());
  }

  public static String findExit(Activity st, Pouch p) {
    if ("switch".equals(st.getType())) {
      if (isLoop(st.getDefaultCondition().getTransition(), p)) {
        String tr = st.getDataConditions().stream().findFirst().orElseThrow().getTransition();
        return tr == null ? tr
            : findIext(
                Set.of(st.getDataConditions().stream().findFirst().orElseThrow().getTransition()),
                p);
      }
      return findIext(st.findPosibleTransitions(), p);
    } else {
      return st.getTransition();
    }
  }
  
  public static String findCommentSection(String text) {
    Objects.requireNonNull(text);
    if(!text.contains(Const.Plant.DOC_START) || !text.contains(Const.Plant.DOC_END)) {
      throw new PlantUmlSyntaxError(Errors2.INVALID_PLANT);
    }
    String src = text.trim();
    src = src.substring(0, src.indexOf(Const.Plant.DOC_END) + Const.Plant.DOC_END.length());
    int i = text.indexOf(Const.Plant.DOC_END) + Const.Plant.DOC_END.length();
    src =  text.substring(i);
    return src;
  }

  public static String findTailJsonText(String text) {
    text = findCommentSection(text);
    if(!text.contains(Const.Plant.LQ_COMMENT) || !text.contains(Const.Plant.RQ_COMMENT)) {
      throw new PlantUmlSyntaxError(Errors2.INVALID_PLANT);
    }
    text = text.substring(text.indexOf(Const.Plant.LQ_COMMENT) + Const.Plant.LQ_COMMENT.length(), 
        text.indexOf(Const.Plant.RQ_COMMENT));
    return text;
  }
  
}
