package validation.orchestrator.service;

public record TestSummary(
    Integer parityPercent,
    int testsTotal,
    int testsPassed,
    int testsFailed,
    String summary
) {
}
