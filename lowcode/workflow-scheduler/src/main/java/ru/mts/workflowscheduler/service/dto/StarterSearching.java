package ru.mts.workflowscheduler.service.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class StarterSearching {

  private Integer limit = 100;
  private Integer offset = 0;
  private String name;
  private String tenantId;
  private List<Sorting> sorting;
  private List<UUID> workflowDefinitionToStartIds;
  private List<String> desiredStatuses;
  private List<String> actualStatuses;

  @Data
  public static class Sorting {
    private String name;
    private String direction;
  }


}
