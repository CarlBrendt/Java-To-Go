package ru.mts.ip.validation.workflowscheduler;

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
        return System.getProperty(JAVA_PROPERTY, "http://workflow-scheduler-java:9016");
    }

    static String goBaseUrl() {
        return System.getProperty(GO_PROPERTY, "http://workflow-scheduler-go:8080");
    }
}
