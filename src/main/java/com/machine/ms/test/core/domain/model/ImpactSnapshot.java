package com.machine.ms.test.core.domain.model;

import java.time.Instant;
import java.util.List;

public record ImpactSnapshot(String impactId,
                             StepKey key,
                             List<ImpactedTest> tests,
                             Instant generatedAt,
                             String requestId) {
}
