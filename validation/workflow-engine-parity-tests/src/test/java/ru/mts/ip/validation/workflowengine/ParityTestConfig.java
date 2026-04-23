package ru.mts.ip.validation.workflowengine;

final class ParityTestConfig {

    private static final String ENABLED_PROPERTY = "parity.enabled";
    private static final String JAVA_PROPERTY = "parity.java.base-url";
    private static final String GO_PROPERTY = "parity.go.base-url";
    private static final String ENABLED_ENV = "PARITY_ENABLED";
    private static final String JAVA_ENV = "JAVA_BASE_URL";
    private static final String GO_ENV = "GO_BASE_URL";

    private ParityTestConfig() {
    }

    static boolean parityEnabled() {
        String value = System.getProperty(ENABLED_PROPERTY);
        if (value == null || value.isBlank()) {
            value = System.getenv(ENABLED_ENV);
        }
        return Boolean.parseBoolean(value);
    }

    static String javaBaseUrl() {
        return readRequired(JAVA_PROPERTY, JAVA_ENV);
    }

    static String goBaseUrl() {
        return readRequired(GO_PROPERTY, GO_ENV);
    }

    private static String readRequired(String propertyName, String envName) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            value = System.getenv(envName);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "Missing test endpoint configuration. Set system property '" + propertyName
                    + "' or environment variable '" + envName + "'"
            );
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
