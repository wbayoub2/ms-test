package com.machine.ms.test.api.contract.functional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ImpactComputeRequest(@NotBlank String service,
                                   @NotBlank String branch,
                                   @NotBlank String baseCommit,
                                   @NotBlank String headCommit,
                                   @NotEmpty List<@Valid ImpactChangeDto> changes) {
}
