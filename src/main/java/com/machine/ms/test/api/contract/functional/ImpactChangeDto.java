package com.machine.ms.test.api.contract.functional;

import jakarta.validation.constraints.NotBlank;

public record ImpactChangeDto(@NotBlank String filePath,
                              int lineStart,
                              int lineEnd) {
}
