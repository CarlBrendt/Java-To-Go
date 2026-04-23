package ru.mts.workflowmail.service.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ConsumerError {
  private UUID workerId;
  private Throwable error;
}
