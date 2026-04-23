package ru.mts.ip.validation.workflowmail;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkflowMailParityTest {

    private static final String VALIDATE_CONSUMER_PATH = "/api/v1/consumer/validate";

    @Test
    void validMailConsumerConfig_shouldMatchBetweenJavaAndGo() throws IOException, InterruptedException {
        assumeParityEnabled();
        WorkflowMailClient javaClient = javaClient();
        WorkflowMailClient goClient = goClient();

        JsonNode body = TestDataSupport.readJson("TestData/case1/valid-consumer.json");
        HttpResponseData javaResponse = javaClient.postJson(VALIDATE_CONSUMER_PATH, body);
        HttpResponseData goResponse = goClient.postJson(VALIDATE_CONSUMER_PATH, body);

        assertEquals(javaResponse.statusCode(), goResponse.statusCode(), "Status mismatch for valid mail consumer config");
        JsonParityComparator.assertJsonEquals(javaResponse.body(), goResponse.body(), "Body mismatch for valid mail consumer config");
    }

    @Test
    void emptyMailConsumerConfig_shouldMatchValidationError() throws IOException, InterruptedException {
        assumeParityEnabled();
        WorkflowMailClient javaClient = javaClient();
        WorkflowMailClient goClient = goClient();

        JsonNode body = TestDataSupport.readJson("TestData/case2/empty-config.json");
        HttpResponseData javaResponse = javaClient.postJson(VALIDATE_CONSUMER_PATH, body);
        HttpResponseData goResponse = goClient.postJson(VALIDATE_CONSUMER_PATH, body);

        assertEquals(javaResponse.statusCode(), goResponse.statusCode(), "Status mismatch for empty mail consumer config");
        JsonParityComparator.assertJsonEquals(javaResponse.body(), goResponse.body(), "Body mismatch for empty mail consumer config");
    }

    private WorkflowMailClient javaClient() {
        return new WorkflowMailClient(ParityTestConfig.javaBaseUrl(), "Java");
    }

    private WorkflowMailClient goClient() {
        return new WorkflowMailClient(ParityTestConfig.goBaseUrl(), "Go");
    }

    private void assumeParityEnabled() {
        Assumptions.assumeTrue(
            ParityTestConfig.parityEnabled(),
            "Parity tests are disabled. Set parity.enabled=true to run them."
        );
    }
}
