package validation.orchestrator.strategy;

import java.nio.file.Path;

public record ValidationExecutionContext(
    Path repoRoot,
    Path composeFile,
    Path workspaceRoot,
    String artifactBaseUrl,
    String validationNetwork
) {
}
