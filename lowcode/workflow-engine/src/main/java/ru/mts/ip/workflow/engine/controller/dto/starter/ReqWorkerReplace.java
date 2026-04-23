package ru.mts.ip.workflow.engine.controller.dto.starter;

import lombok.Data;

@Data
public class ReqWorkerReplace {
  private Integer retryCount;
  private String status;
}
