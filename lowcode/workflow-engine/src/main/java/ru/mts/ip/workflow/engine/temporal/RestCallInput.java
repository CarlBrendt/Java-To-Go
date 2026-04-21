package ru.mts.ip.workflow.engine.temporal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import lombok.Data;
import ru.mts.ip.workflow.engine.dto.Ref;
import ru.mts.ip.workflow.engine.dto.XsdValidation;

@JsonInclude(Include.NON_NULL)
public record RestCallInput(String activityId, RestCallConfig restCallConfig,
    ScriptExecutionContext context, OutputValidation outputValidation,
    RestCallResultMock mockResult) {

  @JsonInclude(Include.NON_NULL)
  public record OutputValidation(String contentType, JsonNode jsonSchema, Script script) {
  }


  @JsonInclude(Include.NON_NULL)
  public record Script(String type, String expression) {
  }

  public record RestCallConfig(Ref restCallTemplateRef,
      RestCallInput.RestCallConfig.ActivityRestCallTemplate restCallTemplateDef,
      List<String> exposedHeaders, List<ManualHandling> resultHandlers) {


    @JsonInclude(Include.NON_NULL)
    public record ActivityRestCallTemplate(String method, String url, JsonNode bodyTemplate,
        Map<String, String> headers, String curl, AuthDef authDef) {
    }


    @JsonInclude(Include.NON_NULL)
    public record AuthDef(String type, Oauth2Details oauth2, BasicDetails basic,
        List<String> tags) {
    }


    @JsonInclude(Include.NON_NULL)
    public record ManualHandling(HttpPredicate predicate, Validation validation) {
    }


    @JsonInclude(Include.NON_NULL)
    public record Validation(JsonNode jsonSchema, XsdValidation xsdValidation) {
    }


    @JsonInclude(Include.NON_NULL)
    public record HttpPredicate(Integer respCode, Set<Integer> respCodes,
        IntInterval respCodeInterval, List<PathValueValidation> respValueAnyOf) {
    }


    @JsonInclude(Include.NON_NULL)
    public record PathValueValidation(String jsonPath, Set<ValueNode> values,
        RestCallConfig.PathValueValidation and) {
    }


    @JsonInclude(Include.NON_NULL)
    public record IntInterval(Integer from, Integer to) {
    }


    @JsonInclude(Include.NON_NULL)
    public record Oauth2Details(String issuerLocation, String clientId, String clientSecret,
        String grantType, String scopes) {
    }


    @JsonInclude(Include.NON_NULL)
    record BasicDetails(String login, String password) {
    }

  }


  public record ScriptExecutionContext(JsonNode vars, ScriptWorkflowView wf) {
  }


  public record ScriptWorkflowView(String businessKey, Map<String, String> secrets,
      Map<String, JsonNode> initVariables, Map<String, List<JsonNode>> consumedMessages) {
  }


  @Data
  public static class RestCallResultMock {
    private JsonNode bodyExample;
    private JsonNode bodySchema;
    private Integer respCode;
    private Map<String, List<String>> headers;
  }
}


