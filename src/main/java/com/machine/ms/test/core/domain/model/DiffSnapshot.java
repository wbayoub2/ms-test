package com.machine.ms.test.core.domain.model;

import java.time.Instant;
import java.util.List;

public record DiffSnapshot(String diffId,
                           StepKey key,
                           List<String> changedFiles,
                           Instant generatedAt,
                           String requestId) {
}
