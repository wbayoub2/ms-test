package com.machine.ms.test.api.contract.ingestion;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OpenCloverCoverageLineDto(
        @NotBlank String filePath,
        @NotBlank String className,
        @NotBlank String methodSig,
        @Min(1) int lineNo,
        boolean covered
) {
}
