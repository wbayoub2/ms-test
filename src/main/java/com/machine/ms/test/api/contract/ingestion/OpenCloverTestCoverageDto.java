package com.machine.ms.test.api.contract.ingestion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OpenCloverTestCoverageDto(
        @NotBlank String testId,
        @NotEmpty List<@Valid OpenCloverCoverageLineDto> coverage
) {
}
