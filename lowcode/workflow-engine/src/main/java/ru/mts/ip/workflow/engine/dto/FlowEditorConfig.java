package ru.mts.ip.workflow.engine.dto;

import java.util.Map;

public record FlowEditorConfig(StartMetadata startMetadata, Map<String, ActivityMetadata> activityMetadata) {
  
  public record StartMetadata(String ims) {    
  }
  
  public record ActivityMetadata(String ims) {    
  }

}
