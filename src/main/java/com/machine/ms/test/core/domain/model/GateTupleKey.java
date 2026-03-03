package com.machine.ms.test.core.domain.model;

public record GateTupleKey(String stepId,
                           StepPhase phase,
                           String baseSha,
                           String headSha,
                           String policyId,
                           String runId) {
}
