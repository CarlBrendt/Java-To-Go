package validation.orchestrator.service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import validation.orchestrator.config.OrchestratorProperties;
import validation.orchestrator.model.ValidationRun;
import validation.orchestrator.strategy.ValidationExecutionContext;
import validation.orchestrator.strategy.ValidationStrategy;

@Service
public class ValidationOrchestratorService {
    private final Path repoRoot;
    private final Path composeFile;
    private final Path workspaceRoot;
    private final String defaultStrategyKey;
    private final String artifactBaseUrl;
    private final String validationNetwork;
    private final Map<String, ValidationRun> runs;
    private final Map<String, ValidationStrategy> strategies;
    private final ExecutorService executor;

    public ValidationOrchestratorService(
        OrchestratorProperties properties,
        List<ValidationStrategy> strategyList
    ) {
        this.repoRoot = ValidationPaths.resolveRepoRoot(properties);
        this.composeFile = ValidationPaths.resolveComposeFile(properties);
        this.workspaceRoot = ValidationPaths.resolveWorkspaceRoot(properties);
        this.defaultStrategyKey = properties.getDefaultStrategy();
        this.artifactBaseUrl = properties.getArtifactBaseUrl();
        this.validationNetwork = properties.getValidationNetwork();
        this.runs = new ConcurrentHashMap<>();
        this.strategies = strategyList.stream()
            .collect(Collectors.toUnmodifiableMap(ValidationStrategy::key, Function.identity()));
        this.executor = Executors.newCachedThreadPool();

        if (!strategies.containsKey(defaultStrategyKey)) {
            throw new IllegalStateException(
                "Default validation strategy is not registered: " + defaultStrategyKey
            );
        }
    }

    public synchronized ValidationRun startRun(String requestedRunId, String requestedStrategyKey, String mwsModel) {
        Optional<ValidationRun> activeRun = findActiveRun();
        if (activeRun.isPresent()) {
            return activeRun.get();
        }

        String strategyKey = normalizeBlank(requestedStrategyKey);
        if (strategyKey == null) {
            strategyKey = defaultStrategyKey;
        }
        if (!strategies.containsKey(strategyKey)) {
            throw new IllegalArgumentException("Validation strategy is not registered: " + strategyKey);
        }

        String runId = requestedRunId == null || requestedRunId.isBlank()
            ? "val_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
            : requestedRunId;
        ValidationRun run = new ValidationRun(
            runId,
            null,
            Instant.now().toString(),
            strategyKey,
            normalizeBlank(mwsModel)
        );
        runs.put(runId, run);
        executor.submit(() -> executeRun(run));
        return run;
    }

    public Optional<ValidationRun> getRun(String runId) {
        return Optional.ofNullable(runs.get(runId));
    }

    private void executeRun(ValidationRun run) {
        ValidationStrategy strategy = strategies.get(run.resolvedStrategyKey());
        if (strategy == null) {
            failRun(run, "failed", "Validation strategy is not registered: " + run.resolvedStrategyKey());
            return;
        }

        try {
            strategy.execute(run, new ValidationExecutionContext(
                repoRoot,
                composeFile,
                workspaceRoot,
                artifactBaseUrl,
                validationNetwork
            ));
        } catch (Exception exception) {
            failRun(run, "failed", "Validation execution crashed: " + exception.getMessage());
        }
    }

    private void failRun(ValidationRun run, String stage, String summary) {
        run.update("failed", stage, "failed", null, 0, 0, 0, summary, Instant.now().toString());
    }

    private Optional<ValidationRun> findActiveRun() {
        return runs.values().stream()
            .filter(run -> "queued".equals(run.status()) || "running".equals(run.status()))
            .findFirst();
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
