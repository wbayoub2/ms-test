package com.machine.ms.test.api.contract.integration;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record MsInfraRunRequest(@NotBlank String workflowId,
                                Map<String, Object> inputs,
                                String target,
                                String mode,
                                String runtimeMode,
                                Boolean strict,
                                Boolean keepArtifacts,
                                String runId,
                                int pollAttempts,
                                long pollSleepMs) {
}
