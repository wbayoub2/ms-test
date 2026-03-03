package com.machine.ms.test.core.domain.model;

public record StepKey(String stepId, StepPhase phase, String baseSha, String headSha) {
}
