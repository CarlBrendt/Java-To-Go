
package ru.mts.workflowscheduler.controller.dto;

import java.util.UUID;

public record WorkerError(UUID workerId, UUID executorId, String errorMessage) {
}
