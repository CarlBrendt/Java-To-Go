package validation.orchestrator.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Component;
import validation.orchestrator.model.ValidationRun;
import validation.orchestrator.service.CommandResult;
import validation.orchestrator.service.TestSummary;

@Component
public class WorkflowEngineValidationStrategy implements ValidationStrategy {
    private static final Pattern TEST_SUMMARY_PATTERN = Pattern.compile(
        "Tests run:\\s*(?<total>\\d+),\\s*Failures:\\s*(?<failures>\\d+),\\s*Errors:\\s*(?<errors>\\d+),\\s*Skipped:\\s*(?<skipped>\\d+)"
    );
    private static final Pattern UNREACHABLE_PATTERN = Pattern.compile(
        "(Go service is unreachable[^\\n]*|Java service is unreachable[^\\n]*)"
    );
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration MIGRATION_TIMEOUT = Duration.ofMinutes(15);
    private static final Duration MIGRATION_POLL_INTERVAL = Duration.ofSeconds(5);
    private static final Duration GO_HEALTH_TIMEOUT = Duration.ofSeconds(90);
    private static final Duration GO_HEALTH_POLL_INTERVAL = Duration.ofSeconds(2);
    private static final String GO_CONTAINER_NAME = "workflow-engine-go";
    private static final String REFERENCE_PROJECT_KEY = "workflow-engine";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String key() {
        return REFERENCE_PROJECT_KEY;
    }

    @Override
    public void execute(ValidationRun run, ValidationExecutionContext context) throws Exception {
        Path runWorkspace = context.workspaceRoot().resolve(run.validationRunId()).toAbsolutePath().normalize();
        recreateDirectory(runWorkspace);

        try {
            updateRun(
                run,
                "running",
                "preparing_reference_source",
                null,
                null,
                0,
                0,
                0,
                "Preparing Java reference source",
                null
            );

            Path referenceProjectRoot = resolveReferenceProjectRoot(context.repoRoot());
            Path referenceArchive = runWorkspace.resolve("reference-java.zip");
            archiveReferenceProject(referenceProjectRoot, referenceArchive);

            String migrationUserId = "validation_" + run.validationRunId() + "_" + UUID.randomUUID().toString().substring(0, 8);
            run.assignMigrationUserId(migrationUserId);

            updateRun(
                run,
                "running",
                "starting_migration",
                null,
                null,
                0,
                0,
                0,
                "Uploading Java reference to migration backend",
                null
            );

            uploadReferenceArchive(migrationUserId, context.artifactBaseUrl(), referenceArchive);

            updateRun(
                run,
                "running",
                "waiting_for_go_artifact",
                null,
                null,
                0,
                0,
                0,
                "Waiting for generated Go artifact",
                null
            );

            awaitGeneratedArtifact(migrationUserId, context.artifactBaseUrl());

            updateRun(
                run,
                "running",
                "starting_reference_runtime",
                null,
                null,
                0,
                0,
                0,
                "Starting reference validation runtime",
                null
            );

            CommandResult upResult = runCommand(List.of(
                "docker", "compose", "-f", context.composeFile().toString(), "up", "-d", "workflow-engine-java"
            ));
            if (upResult.exitCode() != 0) {
                failRun(run, "starting_reference_runtime", trimSummary(upResult));
                return;
            }

            updateRun(
                run,
                "running",
                "downloading_go_artifact",
                null,
                null,
                0,
                0,
                0,
                "Downloading generated Go artifact",
                null
            );

            Path artifactZip = runWorkspace.resolve("generated-go.zip");
            downloadGeneratedArtifact(migrationUserId, context.artifactBaseUrl(), artifactZip);

            updateRun(
                run,
                "running",
                "preparing_go_source",
                null,
                null,
                0,
                0,
                0,
                "Preparing Go source workspace",
                null
            );

            Path extractedRoot = extractArtifact(artifactZip, runWorkspace.resolve("extracted"));

            updateRun(
                run,
                "running",
                "building_go_runtime",
                null,
                null,
                0,
                0,
                0,
                "Building generated Go runtime",
                null
            );

            cleanupGoContainer();

            String goImageTag = "workflow-engine-go:" + run.validationRunId();
            CommandResult buildResult = runCommand(List.of(
                "docker", "build", "-t", goImageTag, extractedRoot.toString()
            ));
            if (buildResult.exitCode() != 0) {
                failRun(run, "building_go_runtime", summarizeBuildFailure(buildResult));
                return;
            }

            updateRun(
                run,
                "running",
                "starting_go_runtime",
                null,
                null,
                0,
                0,
                0,
                "Starting generated Go runtime",
                null
            );

            CommandResult runResult = runCommand(List.of(
                "docker", "run", "-d",
                "--name", GO_CONTAINER_NAME,
                "--network", context.validationNetwork(),
                "--network-alias", GO_CONTAINER_NAME,
                "-e", "PORT=8080",
                goImageTag
            ));
            if (runResult.exitCode() != 0) {
                failRun(run, "starting_go_runtime", trimSummary(runResult));
                return;
            }

            String healthFailure = waitForGoHealth();
            if (healthFailure != null) {
                failRun(run, "starting_go_runtime", healthFailure);
                return;
            }

            updateRun(
                run,
                "running",
                "running_parity_tests",
                null,
                null,
                0,
                0,
                0,
                "Running built-in parity tests",
                null
            );

            CommandResult testResult = runCommand(List.of(
                "docker", "compose", "-f", context.composeFile().toString(), "up", "--build", "parity-tests"
            ));

            TestSummary summary = parseTestSummary(testResult);
            if (testResult.exitCode() == 0) {
                updateRun(
                    run,
                    "finished",
                    "finished",
                    "passed",
                    summary.parityPercent(),
                    summary.testsTotal(),
                    summary.testsPassed(),
                    summary.testsFailed(),
                    summary.summary(),
                    Instant.now().toString()
                );
                return;
            }

            updateRun(
                run,
                "failed",
                "failed",
                "failed",
                summary.parityPercent(),
                summary.testsTotal(),
                summary.testsPassed(),
                summary.testsFailed(),
                summary.summary(),
                Instant.now().toString()
            );
        } finally {
            cleanupGoContainer();
        }
    }

    private Path resolveReferenceProjectRoot(Path repoRoot) {
        Path referenceProjectRoot = repoRoot.resolve("lowcode").resolve(REFERENCE_PROJECT_KEY).normalize();
        if (!Files.exists(referenceProjectRoot)) {
            throw new IllegalStateException("Reference project is not available: " + referenceProjectRoot);
        }
        return referenceProjectRoot;
    }

    private void archiveReferenceProject(Path sourceRoot, Path targetZip) throws IOException {
        Files.createDirectories(targetZip.getParent());
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(targetZip))) {
            try (var walk = Files.walk(sourceRoot)) {
                walk.sorted()
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> writeZipEntry(sourceRoot, path, zipOutputStream));
            }
        }
    }

    private void writeZipEntry(Path sourceRoot, Path file, ZipOutputStream zipOutputStream) {
        String entryName = sourceRoot.getFileName() + "/" + sourceRoot.relativize(file).toString().replace('\\', '/');
        try {
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            Files.copy(file, zipOutputStream);
            zipOutputStream.closeEntry();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot archive reference file: " + file, exception);
        }
    }

    private void uploadReferenceArchive(String migrationUserId, String artifactBaseUrl, Path referenceArchive)
        throws IOException, InterruptedException {
        String boundary = "----JavaToGoBoundary" + UUID.randomUUID();
        byte[] fileBytes = Files.readAllBytes(referenceArchive);

        List<byte[]> bodyParts = new ArrayList<>();
        bodyParts.add(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        bodyParts.add((
            "Content-Disposition: form-data; name=\"file\"; filename=\"%s\"\r\n".formatted(referenceArchive.getFileName())
        ).getBytes(StandardCharsets.UTF_8));
        bodyParts.add(("Content-Type: application/zip\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        bodyParts.add(fileBytes);
        bodyParts.add("\r\n".getBytes(StandardCharsets.UTF_8));
        bodyParts.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(
                artifactBaseUrl
                    + "/api/v1/minio/minio-upload-zip?user_id="
                    + URLEncoder.encode(migrationUserId, StandardCharsets.UTF_8)
                    + "&auto_migrate=true"
            ))
            .timeout(HTTP_TIMEOUT)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArrays(bodyParts))
            .build();

        HttpResponse<String> response = httpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Reference upload failed: HTTP " + response.statusCode() + " " + response.body());
        }
    }

    private void awaitGeneratedArtifact(String migrationUserId, String artifactBaseUrl)
        throws IOException, InterruptedException {
        Instant deadline = Instant.now().plus(MIGRATION_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                    artifactBaseUrl
                        + "/api/v1/minio/migrate/status?user_id="
                        + URLEncoder.encode(migrationUserId, StandardCharsets.UTF_8)
                ))
                .timeout(HTTP_TIMEOUT)
                .GET()
                .build();

            HttpResponse<String> response = httpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Migration status check failed: HTTP " + response.statusCode());
            }

            JsonNode statusNode = objectMapper.readTree(response.body());
            String status = statusNode.path("status").asText();
            if ("completed".equals(status) && statusNode.path("has_zip").asBoolean(false)) {
                return;
            }
            if ("error".equals(status)) {
                throw new IllegalStateException(statusNode.path("message").asText("Migration failed"));
            }

            Thread.sleep(MIGRATION_POLL_INTERVAL.toMillis());
        }

        throw new IllegalStateException("Migration timed out while waiting for generated Go artifact");
    }

    private void downloadGeneratedArtifact(String migrationUserId, String artifactBaseUrl, Path targetZip)
        throws IOException, InterruptedException {
        String downloadUrl = artifactBaseUrl
            + "/api/v1/minio/minio-download-ready-zip?user_id="
            + URLEncoder.encode(migrationUserId, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(downloadUrl))
            .timeout(HTTP_TIMEOUT)
            .GET()
            .build();

        HttpResponse<byte[]> response = httpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 404) {
            throw new IllegalStateException("Generated Go ZIP not found for migration user " + migrationUserId);
        }
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Failed to download generated Go ZIP: HTTP " + response.statusCode());
        }

        Files.write(targetZip, response.body());
    }

    private Path extractArtifact(Path artifactZip, Path extractionRoot) throws IOException {
        recreateDirectory(extractionRoot);

        Path detectedProjectRoot = null;
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(artifactZip))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (entryName == null || entryName.isBlank() || entryName.startsWith("__MACOSX__")) {
                    continue;
                }

                Path targetPath = extractionRoot.resolve(entryName).normalize();
                if (!targetPath.startsWith(extractionRoot)) {
                    throw new IllegalStateException("ZIP entry escapes extraction root: " + entryName);
                }

                if (detectedProjectRoot == null) {
                    String topLevel = entryName.split("/", 2)[0];
                    detectedProjectRoot = extractionRoot.resolve(topLevel).normalize();
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                    continue;
                }

                Files.createDirectories(targetPath.getParent());
                Files.copy(zipInputStream, targetPath);
            }
        }

        if (detectedProjectRoot == null || !Files.exists(detectedProjectRoot)) {
            throw new IllegalStateException("Generated Go ZIP does not contain a project root");
        }
        if (!Files.exists(detectedProjectRoot.resolve("Dockerfile"))) {
            throw new IllegalStateException("Generated Go project does not contain Dockerfile");
        }

        return detectedProjectRoot;
    }

    private String waitForGoHealth() throws IOException, InterruptedException {
        Instant deadline = Instant.now().plus(GO_HEALTH_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            CommandResult inspectResult = runCommand(List.of(
                "docker", "inspect",
                "--format", "{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}",
                GO_CONTAINER_NAME
            ));

            if (inspectResult.exitCode() != 0) {
                return "Go runtime container is not available: " + trimSummary(inspectResult);
            }

            String health = inspectResult.stdout().trim();
            if ("healthy".equals(health)) {
                return null;
            }
            if ("exited".equals(health) || "dead".equals(health)) {
                CommandResult logsResult = runCommand(List.of("docker", "logs", GO_CONTAINER_NAME));
                return "Go runtime failed to start: " + trimSummary(logsResult);
            }

            Thread.sleep(GO_HEALTH_POLL_INTERVAL.toMillis());
        }

        CommandResult logsResult = runCommand(List.of("docker", "logs", GO_CONTAINER_NAME));
        return "Go runtime healthcheck timed out: " + trimSummary(logsResult);
    }

    private void cleanupGoContainer() throws IOException, InterruptedException {
        runCommand(List.of("docker", "rm", "-f", GO_CONTAINER_NAME));
    }

    private void recreateDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            try (var walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(file -> {
                        if (!file.delete()) {
                            throw new IllegalStateException("Cannot delete workspace path: " + file.getAbsolutePath());
                        }
                    });
            }
        }
        Files.createDirectories(path);
    }

    private void failRun(ValidationRun run, String stage, String summary) {
        updateRun(run, "failed", stage, "failed", null, 0, 0, 0, summary, Instant.now().toString());
    }

    private void updateRun(
        ValidationRun run,
        String status,
        String stage,
        String result,
        Integer parityPercent,
        int testsTotal,
        int testsPassed,
        int testsFailed,
        String summary,
        String finishedAt
    ) {
        run.update(status, stage, result, parityPercent, testsTotal, testsPassed, testsFailed, summary, finishedAt);
    }

    private CommandResult runCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        String stdout = readAll(process.getInputStream());
        int exitCode = process.waitFor();

        return new CommandResult(exitCode, stdout, "");
    }

    private TestSummary parseTestSummary(CommandResult result) {
        String output = result.stdout() + "\n" + result.stderr();
        Matcher summaryMatcher = TEST_SUMMARY_PATTERN.matcher(output);
        if (!summaryMatcher.find()) {
            return new TestSummary(null, 0, 0, 0, trimSummary(result));
        }

        int testsTotal = Integer.parseInt(summaryMatcher.group("total"));
        int testsFailed = Integer.parseInt(summaryMatcher.group("failures"))
            + Integer.parseInt(summaryMatcher.group("errors"));
        int testsPassed = Math.max(testsTotal - testsFailed, 0);
        int parityPercent = testsTotal == 0 ? 0 : (testsPassed * 100) / testsTotal;

        Matcher unreachableMatcher = UNREACHABLE_PATTERN.matcher(output);
        String summary = unreachableMatcher.find()
            ? unreachableMatcher.group(1).trim()
            : testsPassed + "/" + testsTotal + " parity tests passed";

        return new TestSummary(parityPercent, testsTotal, testsPassed, testsFailed, summary);
    }

    private String summarizeBuildFailure(CommandResult result) {
        String output = result.stdout() == null ? "" : result.stdout();
        List<String> highlights = output.lines()
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .filter(line ->
                line.contains("undefined: ")
                    || line.contains("redeclared")
                    || line.contains("syntax error")
                    || line.contains("cannot use ")
                    || line.contains("missing return")
                    || line.contains("too many arguments")
            )
            .limit(3)
            .toList();

        if (!highlights.isEmpty()) {
            return "Go candidate build failed: " + String.join("; ", highlights);
        }

        return "Go candidate build failed: " + trimSummary(result);
    }

    private String trimSummary(CommandResult result) {
        String raw = (result.stderr() == null || result.stderr().isBlank()) ? result.stdout() : result.stderr();
        if (raw == null || raw.isBlank()) {
            return "Validation run failed";
        }

        String[] lines = raw.trim().split("\\R");
        return lines[lines.length - 1];
    }

    private String readAll(InputStream inputStream) throws IOException {
        try (inputStream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            inputStream.transferTo(output);
            return output.toString(StandardCharsets.UTF_8);
        }
    }

    private HttpClient httpClient() {
        return HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .build();
    }
}
