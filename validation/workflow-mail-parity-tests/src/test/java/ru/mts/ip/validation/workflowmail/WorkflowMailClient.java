package ru.mts.ip.validation.workflowmail;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

final class WorkflowMailClient {

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String systemName;

    WorkflowMailClient(String baseUrl, String systemName) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.systemName = systemName;
    }

    HttpResponseData postJson(String path, JsonNode body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(TestDataSupport.MAPPER.writeValueAsString(body)))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode responseBody = parseBody(response.body());
            return new HttpResponseData(response.statusCode(), responseBody);
        } catch (ConnectException exception) {
            throw new IllegalStateException(systemName + " service is unreachable at " + baseUrl, exception);
        }
    }

    private JsonNode parseBody(String rawBody) throws IOException {
        if (rawBody == null || rawBody.isBlank()) {
            return TestDataSupport.MAPPER.nullNode();
        }
        return TestDataSupport.MAPPER.readTree(rawBody);
    }
}
