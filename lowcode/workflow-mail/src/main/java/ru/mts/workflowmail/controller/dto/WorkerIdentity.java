package ru.mts.workflowmail.controller.dto;

import java.util.UUID;


public record WorkerIdentity(UUID workerId, UUID executorId) {
}
