package com.machine.ms.test.core.domain.model;

import java.time.Instant;
import java.util.List;

public record TestRunSnapshot(String ingestionId,
                              String requestId,
                              String service,
                              String branch,
                              String commitSha,
                              String runId,
                              String launchId,
                              String status,
                              java.util.List<TestResult> results,
                              Instant ingestedAt) {

    public long failedCount() {
        return results.stream().filter(result -> "FAILED".equalsIgnoreCase(result.status())).count();
    }
}
