package com.machine.ms.test.api.contract.ingestion;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReportPortalTestResultDto(
        @NotBlank String testId,
        @NotBlank String status,
        @Min(0) long durationMs,
        String errorSignature
) {
}
