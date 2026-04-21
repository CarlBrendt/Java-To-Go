package ru.mts.ip.workflow.engine.esql;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
public class WorfklowDefinitionHelper {
  
  private final JsonValueWrapper json;
  
  public ActivityWrapper createInject(Map<String, String> injectStringMap, String descritpion) {
    var res = ActivityWrapper.createInject(injectStringMap, descritpion);
    saveActivity(res);
    return res;
  }

  public ActivityWrapper createParallel(List<String> flows) {
    var res = ActivityWrapper.createParallel(flows);
    saveActivity(res);
    return res;
  }

  public ActivityWrapper createSwitch(SwitchEntry def, List<SwitchEntry> others) {
    var res = ActivityWrapper.createSwitch(def, others);
    saveActivity(res);
    return res;
  }

  @Data
  @Accessors(chain = true)
  public static class SwitchEntry {
    private String transition;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String condition;
  } 
  
  public JsonNode asNode() {
    return json.getNode();
  }

  public void insertActivityAsNext(ActivityWrapper activity, String parentId) {
    if(parentId == null) {
      setStart(activity.getId());
    } else {
      ActivityWrapper parent = findActivityById(parentId).orElseThrow(() -> new IllegalArgumentException("Activity[%s] is not found".formatted(parentId)));
      parent.setTransition(activity.getId());
    }
  }
  
  private void setStart(String id) {
    json.get("compiled").put("start", id);
  }

  public void saveActivity(ActivityWrapper activity) {
    json.get("compiled").get("activities").add(activity.asNode());
  }
  
  public Optional<ActivityWrapper> findActivityById(String id) {
    var activity = json.get("compiled").get("activities").findByFieldValue("id", id);
    if(activity.isExists()) {
      return Optional.of(new ActivityWrapper(new JsonValueWrapper(activity.getNode())));
    }
    return Optional.empty();
  }

  public WorfklowDefinitionHelper(JsonNode json) {
    this.json = new JsonValueWrapper(json);
  }
  
  @SneakyThrows
  public static void main(String[] args) {
    String def = """
    {
        "type": "complex",
        "name": "cpic_",
        "compiled": {
            "start": "1",
            "activities": [
                {
                    "id": "1",
                    "type": "inject",
                    "scoped": false,
                    "injectData": {
                        "a": "12"
                    },
                    "transition": "2"
                },
                {
                    "id": "2",
                    "type": "inject",
                    "scoped": false,
                    "injectData": {
                        "b": "12"
                    },
                    "transition": "3"
                },
                {
                    "id": "3",
                    "type": "inject",
                    "scoped": false,
                    "injectData": {
                        "c": "jp{b}"
                    }
                }
            ]
        }
    }            
    """;
    WorfklowDefinitionHelper workflowHelper = new WorfklowDefinitionHelper(new ObjectMapper().readTree(def));
    var inject = workflowHelper.createInject(Map.of("esqlCompileResult", "lua{%s}lua".formatted("return false")), "");
    workflowHelper.insertActivityAsNext(inject, "3");
    System.out.println(workflowHelper.asNode());
  }

  @RequiredArgsConstructor
  public static class ActivityWrapper {
    
    private final JsonValueWrapper wrapper;

    public String getId() {
      return wrapper.get("id").asString().orElseThrow(() -> new IllegalArgumentException("Activity id is not found"));
    }
    
    JsonNode asNode() {
      return wrapper.getNode();
    }

    public void setTransition(String activityId) {
      wrapper.put("transition", activityId);
    }

    public Optional<String> getTransition() {
      return wrapper.get("transition").asString();
    }

    static ActivityWrapper createInject(Map<String, String> injectStringMap, String description) {
      JsonValueWrapper wrapper = new JsonValueWrapper();
      wrapper.put("id", "synt_%s".formatted(UUID.randomUUID().toString()));
      wrapper.put("type", "inject");
      wrapper.put("injectData", injectStringMap);
      wrapper.put("description", description);
      return new ActivityWrapper(wrapper); 
    }

    static ActivityWrapper createSwitch(SwitchEntry def, List<SwitchEntry> others) {
      JsonValueWrapper wrapper = new JsonValueWrapper();
      wrapper.put("id", "synt_%s".formatted(UUID.randomUUID().toString()));
      wrapper.put("type", "switch");
      wrapper.put("defaultCondition", def);
      wrapper.put("dataConditions", others);
      return new ActivityWrapper(wrapper); 
    }

    static ActivityWrapper createParallel(List<String> activityIds) {
      JsonValueWrapper wrapper = new JsonValueWrapper();
      wrapper.put("id", "synt_%s".formatted(UUID.randomUUID().toString()));
      wrapper.put("type", "parallel");
      wrapper.put("branches", activityIds);
      wrapper.put("completionType", "allOf");
      return new ActivityWrapper(wrapper); 
    }
    
  }
  
}
