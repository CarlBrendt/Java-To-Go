package ru.mts.workflowscheduler.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.mts.workflowscheduler.share.script.ScriptExecutionContext;

import java.util.Map;

@FeignClient(value = "wf-script-executor-client")
public interface ScriptExecutorClient {

  @PostMapping(value = "/api/v1/placeholders/resolve")
  ResResolvePlaceholdersExecutionResult resolvePlaceholders(@RequestBody ReqResolvePlaceholdersExecutionContext request);
  @PostMapping(value = "/api/v1/placeholders/is-executable")
  ResIsExecutable isExecutable(@RequestBody ReqIsExecutable request);
  @PostMapping(value = "/api/v1/script-contexts/inject")
  ScriptExecutionContext inject(@RequestBody ReqResolvePlaceholdersExecutionContext request);
  @PostMapping(value = "/api/v1/variables/filter")
  ResResolvePlaceholdersExecutionResult filterOutput(@RequestBody ReqFilterOutput request);

  @Data
  @Accessors(chain = true)
  public static class ReqIsExecutable {
    private String script;
  }

  @Data
  public static class ResIsExecutable {
    private Boolean result;
  }

  @Data
  public static class ReqResolvePlaceholdersExecutionContext {

    private ReqScriptExecutionContext scriptContext;
    private JsonNode node;

    @Data
    public static class ReqScriptExecutionContext {

      private JsonNode vars;
      private ReqScriptWorkflowView wf;

      @Data
      public static class ReqScriptWorkflowView {
        private String businessKey;
        private Map<String, String> secrets;
        private Map<String, JsonNode> initVariables;
      }

    }

  }

  @Data
  @Accessors(chain = true)
  public static class ResResolvePlaceholdersExecutionResult {
    private JsonNode resultNode;
  }

  @Data
  @Accessors(chain = true)
  public static class ReqFilterOutput {
    private Map<String, String> filter;
    private ScriptExecutionContext ctx;
    private JsonNode output;
  }
}
