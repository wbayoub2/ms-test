package com.machine.ms.test.core.domain.model;

import java.time.Instant;
import java.util.List;

public record CoverageSnapshot(String ingestionId,
                               String requestId,
                               String service,
                               String repository,
                               String branch,
                               String commitSha,
                               List<TestCoverageRecord> tests,
                               Instant ingestedAt) {
}
