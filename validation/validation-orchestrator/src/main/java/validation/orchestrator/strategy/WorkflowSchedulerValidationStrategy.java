package validation.orchestrator.strategy;

import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
public class WorkflowSchedulerValidationStrategy extends WorkflowEngineValidationStrategy {
    @Override
    protected String referenceProjectKey() {
        return "workflow-scheduler";
    }

    @Override
    protected String referenceJavaServiceName() {
        return "workflow-scheduler-java";
    }

    @Override
    protected String goContainerName() {
        return "workflow-scheduler-go";
    }

    @Override
    protected String parityJavaBaseUrl() {
        return "http://workflow-scheduler-java:9016";
    }

    @Override
    protected String parityGoBaseUrl() {
        return "http://workflow-scheduler-go:8080";
    }

    @Override
    protected String parityTestSelector() {
        return "WorkflowSchedulerParityTest";
    }

    @Override
    protected Path validationComposeFile(ValidationExecutionContext context) {
        return context.repoRoot()
            .resolve("validation")
            .resolve("workflow-scheduler-parity-tests")
            .resolve("validation-stack")
            .resolve("docker-compose.yml")
            .toAbsolutePath()
            .normalize();
    }
}
