package validation.orchestrator.api;

public record ValidationRunCreateResponse(
    String validationRunId,
    String resolvedStrategyKey,
    String status,
    String summary
) {
}
