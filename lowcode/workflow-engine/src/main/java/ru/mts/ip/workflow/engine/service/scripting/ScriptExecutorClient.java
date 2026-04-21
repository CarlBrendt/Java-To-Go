package ru.mts.ip.workflow.engine.service.scripting;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.fasterxml.jackson.databind.JsonNode;
import feign.Response;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.exception.ScriptExecutionContext;

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
  @PostMapping(value = "/api/v1/placeholders/resolve", produces = "application/json")
  Response resolvePlaceholdersProxy(@RequestBody ReqResolvePlaceholdersExecutionContext request);
  
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
        private Instant workflowExpiration;
        private Map<String, String> secrets;
        private Map<String, JsonNode> initVariables;
        private Map<String, List<JsonNode>> consumedMessages;
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
