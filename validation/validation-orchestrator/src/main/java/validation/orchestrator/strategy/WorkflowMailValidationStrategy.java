package validation.orchestrator.strategy;

import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
public class WorkflowMailValidationStrategy extends WorkflowEngineValidationStrategy {
    @Override
    protected String referenceProjectKey() {
        return "workflow-mail";
    }

    @Override
    protected String referenceJavaServiceName() {
        return "workflow-mail-java";
    }

    @Override
    protected String goContainerName() {
        return "workflow-mail-go";
    }

    @Override
    protected String parityJavaBaseUrl() {
        return "http://workflow-mail-java:9018";
    }

    @Override
    protected String parityGoBaseUrl() {
        return "http://workflow-mail-go:8080";
    }

    @Override
    protected String parityTestSelector() {
        return "WorkflowMailParityTest";
    }

    @Override
    protected Path validationComposeFile(ValidationExecutionContext context) {
        return context.repoRoot()
            .resolve("validation")
            .resolve("workflow-mail-parity-tests")
            .resolve("validation-stack")
            .resolve("docker-compose.yml")
            .toAbsolutePath()
            .normalize();
    }
}
