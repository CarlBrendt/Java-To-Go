package ru.mts.ip.workflow.engine.lang.plant;

import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;

@Data
public class PlantTail {

  private String tenantId;
  private String name;
  private String description;
  private String type;
  private Integer version;
  private JsonNode details;
  
  private Map<String, JsonNode> activities = new LinkedHashMap<>();

  public PlantTail() {}

  public PlantTail(WorkflowDefinition def) {
    tenantId = def.getTenantId();
    name = def.getName();
    description = def.getDescription();
    type = def.getType();
    version = def.getVersion();
    details = def.getDetails();
  }

  public WorkflowDefinition toDefinition() {
    WorkflowDefinition res = new WorkflowDefinition();
    res.setTenantId(tenantId);
    res.setName(name);
    res.setDescription(description);
    res.setType(type);
    res.setDetails(details);
    return res;
  }


}
