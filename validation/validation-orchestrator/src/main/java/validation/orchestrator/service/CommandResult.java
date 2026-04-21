package validation.orchestrator.service;

public record CommandResult(int exitCode, String stdout, String stderr) {
}
