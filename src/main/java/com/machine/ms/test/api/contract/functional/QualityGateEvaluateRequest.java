package com.machine.ms.test.api.contract.functional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QualityGateEvaluateRequest(@NotBlank String service,
                                         @NotBlank String branch,
                                         @NotBlank String baseCommit,
                                         @NotBlank String headCommit,
                                         @NotNull @Valid QualityGatePolicy policy) {
}
