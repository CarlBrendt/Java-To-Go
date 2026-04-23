package ru.mts.workflowscheduler.controller.dto;

import java.util.UUID;


public record WorkerIdentity(UUID workerId, UUID executorId) {
}
