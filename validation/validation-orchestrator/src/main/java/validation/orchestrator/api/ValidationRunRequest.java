package validation.orchestrator.api;

public record ValidationRunRequest(
    String validationRunId,
    String mwsModel
) {
}
