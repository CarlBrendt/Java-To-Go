package ru.mts.ip.workflow.engine.service.starter;

import ru.mts.ip.workflow.engine.entity.StarterTaskEntity;
import ru.mts.ip.workflow.engine.service.dto.StarterTask;

import java.util.UUID;

public interface StarterTaskService {
  StarterTaskEntity save(StarterTask task);
  void processStartWorkflow();

  void stopTask(UUID id);

  StarterTaskEntity restartTask(UUID id);

  StarterTaskEntity getSapTask(UUID id);

  void deleteOldTasks();
}
