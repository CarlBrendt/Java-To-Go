package validation.orchestrator.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import validation.orchestrator.model.ValidationRun;
import validation.orchestrator.service.ValidationOrchestratorService;

@RestController
@RequestMapping("/api/v1/orchestrator")
public class ValidationOrchestratorController {
    private final ValidationOrchestratorService orchestratorService;

    public ValidationOrchestratorController(ValidationOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok");
    }

    @PostMapping("/runs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ValidationRunCreateResponse startRun(@RequestBody(required = false) ValidationRunRequest request) {
        ValidationRun run = orchestratorService.startRun(
            request == null ? null : request.validationRunId()
        );
        return new ValidationRunCreateResponse(
            run.validationRunId(),
            run.resolvedStrategyKey(),
            run.status(),
            run.summary()
        );
    }

    @GetMapping("/runs/{runId}")
    public ValidationRunResponse getRun(@PathVariable String runId) {
        ValidationRun run = orchestratorService.getRun(runId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Validation run not found"));
        return new ValidationRunResponse(
            run.validationRunId(),
            run.resolvedStrategyKey(),
            run.status(),
            run.stage(),
            run.result(),
            run.parityPercent(),
            run.testsTotal(),
            run.testsPassed(),
            run.testsFailed(),
            run.summary(),
            run.migrationUserId(),
            run.startedAt(),
            run.finishedAt()
        );
    }
}
