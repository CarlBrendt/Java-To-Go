package ru.mts.ip.validation.workflowscheduler;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkflowSchedulerParityTest {

    private static final String VALIDATE_STARTER_PATH = "/api/v1/starters/validate";

    @Test
    void validSimpleStarterConfig_shouldMatchBetweenJavaAndGo() throws IOException, InterruptedException {
        assumeParityEnabled();
        WorkflowSchedulerClient javaClient = javaClient();
        WorkflowSchedulerClient goClient = goClient();

        JsonNode body = TestDataSupport.readJson("TestData/case1/valid-simple.json");
        HttpResponseData javaResponse = javaClient.postJson(VALIDATE_STARTER_PATH, body);
        HttpResponseData goResponse = goClient.postJson(VALIDATE_STARTER_PATH, body);

        assertEquals(javaResponse.statusCode(), goResponse.statusCode(), "Status mismatch for valid scheduler config");
        JsonParityComparator.assertJsonEquals(javaResponse.body(), goResponse.body(), "Body mismatch for valid scheduler config");
    }

    @Test
    void emptyStarterConfig_shouldMatchValidationError() throws IOException, InterruptedException {
        assumeParityEnabled();
        WorkflowSchedulerClient javaClient = javaClient();
        WorkflowSchedulerClient goClient = goClient();

        JsonNode body = TestDataSupport.readJson("TestData/case2/empty-config.json");
        HttpResponseData javaResponse = javaClient.postJson(VALIDATE_STARTER_PATH, body);
        HttpResponseData goResponse = goClient.postJson(VALIDATE_STARTER_PATH, body);

        assertEquals(javaResponse.statusCode(), goResponse.statusCode(), "Status mismatch for empty scheduler config");
        JsonParityComparator.assertJsonEquals(javaResponse.body(), goResponse.body(), "Body mismatch for empty scheduler config");
    }

    private WorkflowSchedulerClient javaClient() {
        return new WorkflowSchedulerClient(ParityTestConfig.javaBaseUrl(), "Java");
    }

    private WorkflowSchedulerClient goClient() {
        return new WorkflowSchedulerClient(ParityTestConfig.goBaseUrl(), "Go");
    }

    private void assumeParityEnabled() {
        Assumptions.assumeTrue(
            ParityTestConfig.parityEnabled(),
            "Parity tests are disabled. Set parity.enabled=true to run them."
        );
    }
}
