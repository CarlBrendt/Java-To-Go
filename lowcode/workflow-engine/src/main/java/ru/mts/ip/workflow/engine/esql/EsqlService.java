package ru.mts.ip.workflow.engine.esql;

import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.exception.ClientErrorDescription;
import ru.mts.ip.workflow.engine.exception.ErrorDescription;

public interface EsqlService {
  
  EsqlToLuaTaskState createCompilationTask(EsqlToLuaTask task);
  EsqlToLuaTaskState getCompilationTaskState(String id);
//  SseEmitter subscribeToTaskEvents(String id);

  @Data
  @Accessors(chain = true)
  public static class EsqlToLuaTask {
    private UUID taskId = UUID.randomUUID();
    private JsonNode workflowDefinition;
    private String parentActivityId;
    private List<SourceFile> esqlSources;
  }

  @Data
  @Accessors(chain = true)
  public static class SourceFile {
    private String name;
    private String content;
  }

  @Data
  @Accessors(chain = true)
  public static class EsqlToLuaTaskState {
    private List<ClientErrorDescription> rawErrors;
    private List<ErrorDescription> errors;
    private String taskId;
    private JsonNode workflowDefinition;
    private String status;
  }

}
