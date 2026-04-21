package validation.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "orchestrator")
public class OrchestratorProperties {
    private String repoRoot = "";
    private String composeFile = "";
    private String defaultStrategy = "workflow-engine";
    private String artifactBaseUrl = "http://server:8585";
    private String workspaceRoot = "";
    private String validationNetwork = "validation-stack_default";

    public String getRepoRoot() {
        return repoRoot;
    }

    public void setRepoRoot(String repoRoot) {
        this.repoRoot = repoRoot;
    }

    public String getComposeFile() {
        return composeFile;
    }

    public void setComposeFile(String composeFile) {
        this.composeFile = composeFile;
    }

    public String getDefaultStrategy() {
        return defaultStrategy;
    }

    public void setDefaultStrategy(String defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
    }

    public String getArtifactBaseUrl() {
        return artifactBaseUrl;
    }

    public void setArtifactBaseUrl(String artifactBaseUrl) {
        this.artifactBaseUrl = artifactBaseUrl;
    }

    public String getWorkspaceRoot() {
        return workspaceRoot;
    }

    public void setWorkspaceRoot(String workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }

    public String getValidationNetwork() {
        return validationNetwork;
    }

    public void setValidationNetwork(String validationNetwork) {
        this.validationNetwork = validationNetwork;
    }
}
