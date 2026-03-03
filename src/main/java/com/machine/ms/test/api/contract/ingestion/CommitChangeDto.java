package com.machine.ms.test.api.contract.ingestion;

import jakarta.validation.constraints.NotBlank;

public record CommitChangeDto(@NotBlank String filePath,
                              @NotBlank String methodSig,
                              int lineStart,
                              int lineEnd,
                              @NotBlank String changeType) {
}
