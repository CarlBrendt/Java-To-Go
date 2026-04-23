package ru.mts.ip.validation.workflowscheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

final class TestDataSupport {

    static final ObjectMapper MAPPER = new ObjectMapper();

    private TestDataSupport() {
    }

    static JsonNode readJson(String resourcePath) throws IOException {
        try (InputStream in = TestDataSupport.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Missing test resource: " + resourcePath);
            }
            return MAPPER.readTree(in);
        }
    }
}
