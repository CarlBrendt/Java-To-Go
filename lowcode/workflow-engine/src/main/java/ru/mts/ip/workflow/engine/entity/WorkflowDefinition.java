package ru.mts.ip.workflow.engine.entity;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.entity.converter.FlowEditorConfigConverter;

@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "definition")
public class WorkflowDefinition {

  @Id
  @Column(name = "id")
  private UUID id;

  @NotBlank
  @Column(name = "type", length = 255)
  private String type;
  
  @NotBlank
  @Column(name = "name", length = 255)
  private String name;

  @Column(name = "description", length = 4000)
  private String description;

  @NotBlank
  @Column(name = "tenant_id", length = 255)
  private String tenantId;

  @CreationTimestamp
  @Column(name = "create_time")
  private OffsetDateTime createTime;

  @UpdateTimestamp
  @Column(name = "change_time")
  private OffsetDateTime changeTime;

  @Column(name = "ver")
  private Integer version;

  @NotBlank
  @Column(name = "status")
  private String status;

  @NotBlank
  @Column(name = "owner_login")
  private String ownerLogin;

  @Column(name = "deleted")
  private boolean deleted;

  @JsonIgnore
  @Column(name = "latest")
  private boolean latest;

  @NotBlank
  @Column(name = "availability_status")
  private String availabilityStatus;

  
  public void setDefaults() {
    id = Optional.ofNullable(id).orElse(UUID.randomUUID());
    version = Optional.ofNullable(version).orElse(0);
    tenantId = Optional.ofNullable(tenantId).orElse(Const.DEFAULT_TENANT_ID);
    status = Optional.ofNullable(status).orElse(Const.DefinitionStatus.DRAFT);
    ownerLogin = Optional.ofNullable(ownerLogin).orElse(Const.DEFAULT_DEFINTION_OWNER_LOGIN);
    availabilityStatus = Optional.ofNullable(availabilityStatus).orElse(Const.DefinitionAvailabilityStatus.ACTIVE);
  }

  @Lob
  @Valid
  @Column(name = "details")
  @Convert(converter = FlowEditorConfigConverter.class)
  private JsonNode details;

  @Lob
  @Valid
  @Column(name = "flow_editor_config")
  @Convert(converter = FlowEditorConfigConverter.class)
  private JsonNode flowEditorConfig;

  @Lob
  @Valid
  @Column(name = "compiled")
  @Convert(converter = FlowEditorConfigConverter.class)
  private JsonNode compiled;
  
  
  public interface ValidationGroups {
    public static interface Complex {}
    public static interface Simple {}
  }
  
  public WorkflowDefinition copy() {
    return new WorkflowDefinition().setCompiled(compiled == null ? null : compiled.deepCopy())
        .setCreateTime(createTime).setDescription(description).setId(id)
        .setDetails(details)
        .setName(name)
        .setTenantId(tenantId).setType(type)
        .setVersion(version);
  }
}
