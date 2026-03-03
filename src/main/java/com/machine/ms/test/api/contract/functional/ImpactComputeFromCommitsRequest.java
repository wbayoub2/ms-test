package com.machine.ms.test.api.contract.functional;

import jakarta.validation.constraints.NotBlank;

public record ImpactComputeFromCommitsRequest(@NotBlank String service,
                                              @NotBlank String baseCommit,
                                              @NotBlank String headCommit,
                                              @NotBlank String branch) {
}
