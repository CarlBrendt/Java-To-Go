package ru.mts.ip.workflow.engine.controller;

import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.exception.ErrorDescription;

@Data
@Accessors(chain = true)
public class ActivityExecutionContext {
  
  private List<ErrorDescription> errors;
  private List<PathSctiptContext> flows;
  
  @Data
  @Accessors(chain = true)
  public static class PathSctiptContext {
    private String path;
    private JsonNode context;
  }
  
}
