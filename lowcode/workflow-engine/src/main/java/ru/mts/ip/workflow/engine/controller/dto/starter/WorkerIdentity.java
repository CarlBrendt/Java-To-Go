package ru.mts.ip.workflow.engine.controller.dto.starter;

import java.util.UUID;


public record WorkerIdentity(UUID workerId, UUID executorId) {
}
