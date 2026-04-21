package ru.mts.ip.workflow.engine.lang.plant;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.controller.dto.ResRetryConfig;
import ru.mts.ip.workflow.engine.dto.FailActivityResult;
import ru.mts.ip.workflow.engine.dto.Ref;

@Data
@Accessors(chain = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowExpression {

  private String start;
  private List<Activity> activities;
  
  @Data
  @Accessors(chain = true)
  @JsonInclude(Include.NON_NULL)
  public static class Activity {
    
    private String id;
    private String type;
    private List<String> tags;
    private Boolean scoped;

    private String description;
    private String transition;
    
    private List<String> branches;
    private String completionType;

    private String timerDuration;
     
    @JsonInclude(Include.NON_NULL)
    private  WorkflowCall workflowCall;
    
    private Map<String, JsonNode> injectData;
    private Map<String, String> outputFilter;
    
    private List<DataCondition> dataConditions;
    private DataCondition defaultCondition;
    
    public Set<String> findPosibleTransitions() {
      Set<String> res = new HashSet<>();
      if (!"switch".equals(type)) {
        res.add(transition);
      }
      if (dataConditions != null) {
        dataConditions.forEach(dc -> {
          String tr = dc.getTransition();
          res.add(tr);
        });
      }
      if (defaultCondition != null) {
        String tr = defaultCondition.getTransition();
        res.add(tr);
      }
      return res;
    }

  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  @Accessors(chain = true)
  @JsonInclude(Include.NON_NULL)
  public static class WorkflowCall {
    private Map<String, JsonNode> args;
    private Ref workflowRef;
    private JsonNode workflowDef;
    private ResRetryConfig retryConfig;
    private FailActivityResult failActivityResult;
  }

  @Data
  @Accessors(chain = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(Include.NON_NULL)
  public static class Transform {
    private String type;
    private Map<String, JsonNode> target;
  }
  
  @Data
  @Accessors(chain = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(Include.NON_NULL)
  public static class DataCondition {
    private String id;
    private String condition;
    private String conditionDescription;
    private String successFlowDescription;
    private String transition;
  }
  

}
