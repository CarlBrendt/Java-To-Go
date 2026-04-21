package ru.mts.ip.workflow.engine.controller.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.Const;

@Data
public class ResInspectionResult {

private List<ResIntegration> integrations;
  
  @Data
  @Accessors(chain = true)  
  @JsonInclude(Include.NON_NULL)
  public static class ResIntegration {
    private String direction = Const.IntegrationDirection.OUTPUT;
    private String kind = Const.IntegrationKind.REST;
    private String host;
    private String port;
    private String url;
    private List<String> tags;
    private Integer starterIndex;
    private String activityId;
  }
  
}
