package validation.orchestrator.strategy;

import validation.orchestrator.model.ValidationRun;

public interface ValidationStrategy {
    String key();

    void execute(ValidationRun run, ValidationExecutionContext context) throws Exception;
}
