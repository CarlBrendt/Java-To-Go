package ru.mts.ip.workflow.engine.temporal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RestCallOutput(JsonNode body, String contentType, String respCode, String headers) {
}

