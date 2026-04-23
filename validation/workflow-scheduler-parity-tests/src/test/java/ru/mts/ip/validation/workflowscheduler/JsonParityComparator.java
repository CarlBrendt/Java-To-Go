package ru.mts.ip.validation.workflowscheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class JsonParityComparator {

    private static final Set<String> IGNORED_FIELDS = Set.of(
        "id",
        "timestamp",
        "startUrl"
    );

    private JsonParityComparator() {
    }

    static void assertJsonEquals(JsonNode expected, JsonNode actual, String messagePrefix) {
        JsonNode normalizedExpected = normalize(expected);
        JsonNode normalizedActual = normalize(actual);
        assertEquals(normalizedExpected, normalizedActual, messagePrefix);
    }

    private static JsonNode normalize(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }

        if (node.isObject()) {
            ObjectNode copy = node.deepCopy();
            IGNORED_FIELDS.forEach(copy::remove);
            copy.fieldNames().forEachRemaining(field -> copy.set(field, normalize(copy.get(field))));
            return copy;
        }

        if (node.isArray()) {
            ArrayNode normalized = TestDataSupport.MAPPER.createArrayNode();
            for (JsonNode item : node) {
                normalized.add(normalize(item));
            }
            return normalized;
        }

        return node;
    }
}
