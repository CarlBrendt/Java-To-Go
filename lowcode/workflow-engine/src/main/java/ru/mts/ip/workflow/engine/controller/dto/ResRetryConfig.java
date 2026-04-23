package ru.mts.ip.workflow.engine.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;

public record ResRetryConfig(
    @Schema(type = "string", format = "ISO_8601_duration", example = "P365D")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Duration initialInterval,
    @Schema(type = "string", format = "ISO_8601_duration", example = "P365D")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Duration maxInterval,
    Integer maxAttempts,
    Double backoffCoefficient) {
}
