package com.machine.ms.test.api.contract.ingestion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ArtifactLinksIngestionRequest(
        @NotBlank String requestId,
        @NotBlank String runId,
        @NotEmpty List<@Valid ArtifactLinkDto> artifacts
) {
}
