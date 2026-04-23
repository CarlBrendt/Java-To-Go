package ru.mts.ip.validation.workflowmail;

final class ParityTestConfig {

    private static final String ENABLED_PROPERTY = "parity.enabled";
    private static final String JAVA_PROPERTY = "parity.java.base-url";
    private static final String GO_PROPERTY = "parity.go.base-url";

    private ParityTestConfig() {
    }

    static boolean parityEnabled() {
        return Boolean.parseBoolean(System.getProperty(ENABLED_PROPERTY, "false"));
    }

    static String javaBaseUrl() {
        return System.getProperty(JAVA_PROPERTY, "http://workflow-mail-java:9018");
    }

    static String goBaseUrl() {
        return System.getProperty(GO_PROPERTY, "http://workflow-mail-go:8080");
    }
}
