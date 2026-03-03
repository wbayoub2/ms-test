package com.machine.ms.test.api.contract.ingestion;

import jakarta.validation.constraints.NotBlank;

public record ArtifactLinkDto(@NotBlank String artifactType,
                              @NotBlank String storage,
                              @NotBlank String url,
                              String checksum) {
}
