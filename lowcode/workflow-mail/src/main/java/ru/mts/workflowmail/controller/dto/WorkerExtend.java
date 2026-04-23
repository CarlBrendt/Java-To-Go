package ru.mts.workflowmail.controller.dto;

import java.util.Set;
import java.util.UUID;


public record WorkerExtend(Set<UUID> workerIds, UUID executorId) {
}
