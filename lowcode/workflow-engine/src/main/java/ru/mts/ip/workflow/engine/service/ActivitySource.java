package ru.mts.ip.workflow.engine.service;

import java.util.Optional;
import ru.mts.ip.workflow.engine.lang.plant.WorkflowExpression.Activity;

public interface ActivitySource {
  Optional<Activity> findById(String id);
}
