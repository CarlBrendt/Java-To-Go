package ru.mts.ip.validation.workflowmail;

import com.fasterxml.jackson.databind.JsonNode;

record HttpResponseData(int statusCode, JsonNode body) {
}
