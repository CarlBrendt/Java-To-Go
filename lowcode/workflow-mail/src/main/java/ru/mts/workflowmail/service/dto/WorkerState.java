package ru.mts.workflowmail.service.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class WorkerState {
  private UUID id;
  private String lastException;
}
