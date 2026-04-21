package validation.orchestrator.api;

public record ValidationRunResponse(
    String validationRunId,
    String resolvedStrategyKey,
    String status,
    String stage,
    String result,
    Integer parityPercent,
    int testsTotal,
    int testsPassed,
    int testsFailed,
    String summary,
    String migrationUserId,
    String startedAt,
    String finishedAt
) {
}
