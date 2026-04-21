package ru.mts.ip.workflow.engine.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;

@Getter
@Setter
@Accessors(chain = true)
public class DetailedWorkflowDefinition {

  private UUID id;
  private String type;
  private String name;
  private String description;
  private String tenantId;
  private OffsetDateTime createTime;
  private Integer version;
  private String status;
  private String availabilityStatus;

  private DefinitionDetails details;
  private JsonNode compiled;
  private JsonNode flowEditorConfig;


  public Map<String, Object> asMemo(){
    return Map.of("id", id, "name", name, "tenantId", tenantId, "version", version);
  }

  public void setDefaults() {
    createTime = Optional.ofNullable(createTime).orElse(OffsetDateTime.now());
    version = Optional.ofNullable(version).orElse(0);
    tenantId = Optional.ofNullable(tenantId).orElse(Const.DEFAULT_TENANT_ID);
    status = Optional.ofNullable(status).orElse(Const.DefinitionStatus.DRAFT);
  }



  private static final ObjectMapper OM = new ObjectMapper().findAndRegisterModules();

  public WorkflowDefinition asDefinition() {
    WorkflowDefinition res = new WorkflowDefinition();
    res.setCompiled(compiled);
    res.setDetails(details == null ? null : OM.valueToTree(details));
    res.setFlowEditorConfig(flowEditorConfig);
    res.setId(id);
    res.setType(type);
    res.setName(name);
    res.setDescription(description);
    res.setTenantId(tenantId);
    res.setCreateTime(createTime);
    res.setVersion(version);
    res.setStatus(status);
    return res;
  }
  
}
