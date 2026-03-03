package com.machine.ms.test.api.contract.ingestion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record CommitChangesIngestionRequest(
        @NotBlank String requestId,
        @NotBlank String service,
        @NotBlank String branch,
        @NotBlank @Pattern(regexp = "^[a-f0-9]{7,64}$") String baseCommit,
        @NotBlank @Pattern(regexp = "^[a-f0-9]{7,64}$") String headCommit,
        @NotEmpty List<@Valid CommitChangeDto> changes
) {
}
