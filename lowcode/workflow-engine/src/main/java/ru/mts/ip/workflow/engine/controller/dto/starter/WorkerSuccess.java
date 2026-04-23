package ru.mts.ip.workflow.engine.controller.dto.starter;

import java.util.UUID;


public record WorkerSuccess(UUID workerId, UUID executorId) {
}
