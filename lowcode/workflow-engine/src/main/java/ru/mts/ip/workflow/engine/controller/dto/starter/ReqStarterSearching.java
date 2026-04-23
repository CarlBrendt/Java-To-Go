package ru.mts.ip.workflow.engine.controller.dto.starter;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Data;
import ru.mts.ip.workflow.engine.Const;

import java.util.List;
import java.util.UUID;

@Data
public class ReqStarterSearching {

  @Schema(defaultValue = "100", minimum = "0", maximum = "100")
  private Long limit = 100L;
  @Schema(defaultValue = "0", minimum = "0")
  private Long offset = 0L;
  private String name;
  private String type;
  private String tenantId;
  private List<UUID> workflowDefinitionToStartIds;
  @Schema(defaultValue = """
  [
      {
          "name": "createTime",
          "direction": "desc"
      }
  ]
  """)
  private List<ReqSorting> sorting;
  
  @Schema(allowableValues = {
    Const.StarterStatus.STARTED
  })
  private List<String> desiredStatuses;
  
  @Schema(allowableValues = {
      Const.StarterStatus.STARTED,
      Const.StarterStatus.ERROR
  })
  private List<String> actualStatuses;
  
  @Data
  public static class ReqSorting {
    @Schema(requiredMode = RequiredMode.REQUIRED, defaultValue = "0", minimum = "0", allowableValues = {
        Const.StarterSortingFields.CREATE_TIME,
        Const.StarterSortingFields.DESIRED_STATUS,
    })
    private String name;
    @Schema(requiredMode = RequiredMode.REQUIRED, allowableValues = {
        Const.SortingDirection.ASC,
        Const.SortingDirection.DESC,
    }, example = Const.SortingDirection.DESC)
    private String direction;
  }
 
}
