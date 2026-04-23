package ru.mts.ip.validation.workflowengine;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkflowEngineParityTest {

    private static final String DEFINITION_PATH = "/api/v1/wf/definition";
    private static final String RUN_PATH = "/api/v1/wf/run";

    @Test
    void case1_deployDefinition_shouldMatchBetweenJavaAndGo() throws IOException, InterruptedException {
        assumeParityEnabled();
        WorkflowEngineClient javaClient = javaClient();
        WorkflowEngineClient goClient = goClient();

        JsonNode deployBody = TestDataSupport.readJson("TestData/case1/deploy.json");
        HttpResponseData javaResponse = javaClient.postJson(DEFINITION_PATH, deployBody);
        HttpResponseData goResponse = goClient.postJson(DEFINITION_PATH, deployBody);

        assertEquals(javaResponse.statusCode(), goResponse.statusCode(), "Status mismatch for case1 deploy");
        JsonParityComparator.assertJsonEquals(javaResponse.body(), goResponse.body(), "Body mismatch for case1 deploy");
    }

    @Test
    void case1_runWorkflow_shouldMatchBetweenJavaAndGo() throws IOException, InterruptedException {
        assumeParityEnabled();
        WorkflowEngineClient javaClient = javaClient();
        WorkflowEngineClient goClient = goClient();

        JsonNode deployBody = TestDataSupport.readJson("TestData/case1/deploy.json");
        JsonNode runBody = TestDataSupport.readJson("TestData/case1/run.json");

        HttpResponseData javaDeployResponse = javaClient.postJson(DEFINITION_PATH, deployBody);
        HttpResponseData goDeployResponse = goClient.postJson(DEFINITION_PATH, deployBody);

        assertEquals(javaDeployResponse.statusCode(), goDeployResponse.statusCode(), "Status mismatch for case1 deploy setup");

        HttpResponseData javaRunResponse = javaClient.postJson(RUN_PATH, runBody);
        HttpResponseData goRunResponse = goClient.postJson(RUN_PATH, runBody);

        assertEquals(javaRunResponse.statusCode(), goRunResponse.statusCode(), "Status mismatch for case1 run");
        JsonParityComparator.assertJsonEquals(javaRunResponse.body(), goRunResponse.body(), "Body mismatch for case1 run");
    }

    @Test
    void case4_emptyDefinition_shouldMatchValidationError() throws IOException, InterruptedException {
        assumeParityEnabled();
        WorkflowEngineClient javaClient = javaClient();
        WorkflowEngineClient goClient = goClient();

        JsonNode deployBody = TestDataSupport.readJson("TestData/case4/deploy.json");

        HttpResponseData javaResponse = javaClient.postJson(DEFINITION_PATH, deployBody);
        HttpResponseData goResponse = goClient.postJson(DEFINITION_PATH, deployBody);

        assertEquals(javaResponse.statusCode(), goResponse.statusCode(), "Status mismatch for case4 deploy");
        JsonParityComparator.assertJsonEquals(javaResponse.body(), goResponse.body(), "Body mismatch for case4 deploy");
    }

    private WorkflowEngineClient javaClient() {
        return new WorkflowEngineClient(ParityTestConfig.javaBaseUrl(), "Java");
    }

    private WorkflowEngineClient goClient() {
        return new WorkflowEngineClient(ParityTestConfig.goBaseUrl(), "Go");
    }

    private void assumeParityEnabled() {
        Assumptions.assumeTrue(
            ParityTestConfig.parityEnabled(),
            "Parity tests are disabled. Set parity.enabled=true to run them."
        );
    }
}
