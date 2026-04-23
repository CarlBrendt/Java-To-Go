package validation.orchestrator.service;

import java.nio.file.Files;
import java.nio.file.Path;

import validation.orchestrator.config.OrchestratorProperties;

final class ValidationPaths {
    private ValidationPaths() {
    }

    static Path resolveRepoRoot(OrchestratorProperties properties) {
        String configuredRepoRoot = properties.getRepoRoot() == null ? "" : properties.getRepoRoot().trim();
        if (!configuredRepoRoot.isEmpty()) {
            Path repoRoot = Path.of(configuredRepoRoot).toAbsolutePath().normalize();
            if (Files.exists(repoRoot)) {
                return repoRoot;
            }
        }

        Path cwd = Path.of("").toAbsolutePath().normalize();
        Path[] candidates = new Path[] {
            cwd,
            cwd.getParent() != null ? cwd.getParent() : cwd,
        };

        for (Path candidate : candidates) {
            if (Files.exists(candidate.resolve("lowcode")) && Files.exists(candidate.resolve("docker-compose.yml"))) {
                return candidate;
            }
        }

        throw new IllegalStateException("Cannot resolve repository root");
    }

    static Path resolveComposeFile(OrchestratorProperties properties) {
        String configuredComposeFile = properties.getComposeFile() == null ? "" : properties.getComposeFile().trim();
        if (!configuredComposeFile.isEmpty()) {
            Path composePath = Path.of(configuredComposeFile);
            if (Files.exists(composePath)) {
                return composePath.toAbsolutePath().normalize();
            }
        }

        Path repoRoot = resolveRepoRoot(properties);
        if (Files.exists(repoRoot)) {
            Path candidate = repoRoot
                .resolve("validation")
                .resolve("workflow-engine-parity-tests")
                .resolve("validation-stack")
                .resolve("docker-compose.yml");
            if (Files.exists(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }

        Path cwd = Path.of("").toAbsolutePath().normalize();
        Path[] candidates = new Path[] {
            cwd.resolve("validation").resolve("workflow-engine-parity-tests").resolve("validation-stack").resolve("docker-compose.yml"),
            cwd.resolveSibling("workflow-engine-parity-tests").resolve("validation-stack").resolve("docker-compose.yml"),
            cwd.resolve("workflow-engine-parity-tests").resolve("validation-stack").resolve("docker-compose.yml"),
            cwd.getParent() != null
                ? cwd.getParent().resolve("validation").resolve("workflow-engine-parity-tests").resolve("validation-stack").resolve("docker-compose.yml")
                : cwd,
        };

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }

        throw new IllegalStateException("Cannot resolve validation-stack docker-compose.yml");
    }

    static Path resolveWorkspaceRoot(OrchestratorProperties properties) {
        String configuredWorkspaceRoot = properties.getWorkspaceRoot() == null
            ? ""
            : properties.getWorkspaceRoot().trim();
        if (!configuredWorkspaceRoot.isEmpty()) {
            Path workspaceRoot = Path.of(configuredWorkspaceRoot).toAbsolutePath().normalize();
            ensureDirectory(workspaceRoot);
            return workspaceRoot;
        }

        Path repoRoot = resolveRepoRoot(properties);
        Path workspaceRoot = repoRoot.resolve(".validation-runs").toAbsolutePath().normalize();
        ensureDirectory(workspaceRoot);
        return workspaceRoot;
    }

    private static void ensureDirectory(Path path) {
        try {
            Files.createDirectories(path);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot create validation workspace root: " + path, exception);
        }
    }
}
