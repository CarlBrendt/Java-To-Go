package ru.mts.ip.workflow.engine.dto;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(Include.NON_NULL)
public class Ref {
  
  public Ref copy() {
    return new Ref()
        .setId(id)
        .setName(name)
        .setVersion(version)
        .setTenantId(tenantId)
        ;
  }
  
  private UUID id;
  private String name;
  private Integer version;
  private String tenantId;
}
