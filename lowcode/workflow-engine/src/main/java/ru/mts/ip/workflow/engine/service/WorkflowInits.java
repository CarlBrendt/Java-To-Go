package ru.mts.ip.workflow.engine.service;

import java.util.Set;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;

public record WorkflowInits(Set<String> activityIds,
    Variables initVariables,
    String aggregateActivityId,
    WorkflowDefinition definition,
    Variables continuedVariables
    ) {
}
