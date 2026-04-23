package validation.orchestrator.strategy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.stereotype.Component;
import validation.orchestrator.model.ValidationRun;

@Component
public class WorkflowMailValidationStrategy extends WorkflowEngineValidationStrategy {
    @Override
    protected Path prepareGoArtifact(ValidationRun run, ValidationExecutionContext context, Path runWorkspace) throws Exception {
        Path readyArtifact = context.repoRoot()
            .resolve("lowcode")
            .resolve(referenceProjectKey() + "-go-ready.zip")
            .toAbsolutePath()
            .normalize();
        if (!Files.exists(readyArtifact)) {
            throw new IllegalStateException("Ready Go artifact is not available: " + readyArtifact);
        }

        updateRun(
            run,
            "running",
            "using_ready_go_artifact",
            null,
            null,
            0,
            0,
            0,
            "Using ready Go artifact from lowcode/workflow-mail-go-ready.zip",
            null
        );

        Path artifactZip = runWorkspace.resolve("generated-go.zip");
        Files.copy(readyArtifact, artifactZip, StandardCopyOption.REPLACE_EXISTING);
        return artifactZip;
    }

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
