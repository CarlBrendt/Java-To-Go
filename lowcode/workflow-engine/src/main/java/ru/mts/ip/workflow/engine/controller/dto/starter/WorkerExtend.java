package ru.mts.ip.workflow.engine.controller.dto.starter;

import java.util.List;
import java.util.UUID;


public record WorkerExtend(List<UUID> workerIds, UUID executorId) {
}
