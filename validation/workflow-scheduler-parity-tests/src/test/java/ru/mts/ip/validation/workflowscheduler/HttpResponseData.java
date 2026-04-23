package ru.mts.ip.validation.workflowscheduler;

import com.fasterxml.jackson.databind.JsonNode;

record HttpResponseData(int statusCode, JsonNode body) {
}
