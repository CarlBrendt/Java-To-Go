package ru.mts.ip.workflow.engine.temporal;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public record SendToRabbitmqInput(String activityId, JsonNode connectionDef, Map<String, JsonNode> messageProperties, String exchange, String routingKey, JsonNode message) {
}
