package ru.mts.ip.workflow.engine.controller.dto;

import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class ReqDefinitionSearchingWithPagination {
  
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

}
