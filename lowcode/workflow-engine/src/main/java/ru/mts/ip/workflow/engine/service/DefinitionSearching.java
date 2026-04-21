package ru.mts.ip.workflow.engine.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.Data;

@Data
public class DefinitionSearching {

  private String description;
  private List<String> products;
  private String ownerLogin;
  private List<String> statuses;
  private String name;
  private String version;
  private String tenantId;
  private UUID id;

  private Integer offset;
  private Integer limit;
  
  public void setDefaults() {
    offset = Optional.ofNullable(offset).orElse(0);
    limit = Optional.ofNullable(limit).orElse(100);
  }
  
}
