package ru.mts.ip.workflow.engine.controller.dto;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import ru.mts.ip.workflow.engine.controller.dto.ReqWorkflowExpressionForValidate.ReqActivity;

@Data
public class ReqDebuggingWorkflowExpression {
  
  private String start;
  private List<ReqActivity> activities;
  private List<ReqActivityDebugConfig> activityConfigurations;
  private Map<String, JsonNode> startVariables;
  private String businessKey;
  private JsonNode inputValidateSchema;

  @Data
  public static class ReqActivityDebugConfig{
    private String activityId;
    private ReqRestCallResultMock restCallResultMock;
    private ReqComplexResultMock complexResultMock;
    private ReqAwaitForMessageResultMock awaitForMessageResultMock;
    private ReqDatabaseCallResultMock dbCallResultMock;
  }
  
  @Data
  public static class ReqRestCallResultMock {
    private JsonNode bodyExample;
    private JsonNode bodySchema;
    private Integer respCode;
    private Map<String, List<String>> headers;
  }

  @Data
  public static class ReqDatabaseCallResultMock {
    private JsonNode resultList;
  }

  @Data
  public static class ReqComplexResultMock {
    private JsonNode result;
  }

  @Data
  public static class ReqAwaitForMessageResultMock {
    private JsonNode message;
  }

  @Data
  public static class ReqXsltTransformResultMock {
    private String xsltTransformResult;
  }

}
