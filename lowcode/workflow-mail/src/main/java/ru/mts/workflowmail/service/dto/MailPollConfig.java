package ru.mts.workflowmail.service.dto;

import lombok.Data;

@Data
public class MailPollConfig {
  private long pollDelaySeconds;
  private int maxFetchSize;
}
