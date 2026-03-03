package com.machine.ms.test.core.domain.model;

public record TestResult(String testId,
                         String status,
                         long durationMs,
                         String errorSignature) {
}
