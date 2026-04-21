package validation.orchestrator.model;

public final class ValidationRun {
    private final String validationRunId;
    private final String startedAt;
    private final String resolvedStrategyKey;

    private volatile String migrationUserId;
    private volatile String status;
    private volatile String stage;
    private volatile String result;
    private volatile Integer parityPercent;
    private volatile int testsTotal;
    private volatile int testsPassed;
    private volatile int testsFailed;
    private volatile String summary;
    private volatile String finishedAt;

    public ValidationRun(String validationRunId, String migrationUserId, String startedAt, String resolvedStrategyKey) {
        this.validationRunId = validationRunId;
        this.migrationUserId = migrationUserId;
        this.startedAt = startedAt;
        this.resolvedStrategyKey = resolvedStrategyKey;
        this.status = "queued";
        this.stage = "queued";
        this.summary = "Validation run accepted";
    }

    public String validationRunId() {
        return validationRunId;
    }

    public String migrationUserId() {
        return migrationUserId;
    }

    public void assignMigrationUserId(String migrationUserId) {
        this.migrationUserId = migrationUserId;
    }

    public String startedAt() {
        return startedAt;
    }

    public String resolvedStrategyKey() {
        return resolvedStrategyKey;
    }

    public String status() {
        return status;
    }

    public String stage() {
        return stage;
    }

    public String result() {
        return result;
    }

    public Integer parityPercent() {
        return parityPercent;
    }

    public int testsTotal() {
        return testsTotal;
    }

    public int testsPassed() {
        return testsPassed;
    }

    public int testsFailed() {
        return testsFailed;
    }

    public String summary() {
        return summary;
    }

    public String finishedAt() {
        return finishedAt;
    }

    public void update(
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
        this.status = status;
        this.stage = stage;
        this.result = result;
        this.parityPercent = parityPercent;
        this.testsTotal = testsTotal;
        this.testsPassed = testsPassed;
        this.testsFailed = testsFailed;
        this.summary = summary;
        this.finishedAt = finishedAt;
    }
}
