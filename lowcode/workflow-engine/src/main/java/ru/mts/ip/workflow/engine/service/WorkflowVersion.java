package ru.mts.ip.workflow.engine.service;

import java.util.UUID;

public interface WorkflowVersion {
  Integer getVersion();
  UUID getId();
}
