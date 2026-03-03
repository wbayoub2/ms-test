package com.machine.ms.test.api.contract.step;

import com.machine.ms.test.core.domain.model.StepPhase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record StepContextRequest(
        @NotBlank String stepId,
        @NotNull StepPhase phase,
        @NotBlank @Pattern(regexp = "^[a-f0-9]{7,64}$") String baseSha,
        @NotBlank @Pattern(regexp = "^[a-f0-9]{7,64}$") String headSha,
        @NotBlank String requestId
) {
}
