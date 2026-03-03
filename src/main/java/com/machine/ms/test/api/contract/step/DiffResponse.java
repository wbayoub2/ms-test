package com.machine.ms.test.api.contract.step;

import com.machine.ms.test.core.domain.model.StepPhase;

import java.time.Instant;
import java.util.List;

public record DiffResponse(String requestId,
                           String stepId,
                           StepPhase phase,
                           String baseSha,
                           String headSha,
                           String diffId,
                           int filesChanged,
                           List<String> changedFiles,
                           Instant generatedAt) {
}
