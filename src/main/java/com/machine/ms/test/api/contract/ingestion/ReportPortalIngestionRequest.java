package com.machine.ms.test.api.contract.ingestion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record ReportPortalIngestionRequest(
        @NotBlank String requestId,
        @NotBlank String service,
        @NotBlank String branch,
        @NotBlank @Pattern(regexp = "^[a-f0-9]{7,64}$") String commitSha,
        @NotBlank String runId,
        @NotBlank String launchId,
        @NotBlank String status,
        @NotEmpty List<@Valid ReportPortalTestResultDto> results
) {
}
