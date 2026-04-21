package ru.mts.ip.workflow.engine.service;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WorkflowIstanceIdentity {
  private String businessKey;
  private String runId;
  
  public WorkflowIstanceIdentity(String businessKey, String runId){
    this.businessKey = businessKey;
    this.runId = runId;
  }

  public WorkflowIstanceIdentity(String businessKey){
    this(businessKey, null);
  }
}
