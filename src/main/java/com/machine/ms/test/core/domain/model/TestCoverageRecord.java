package com.machine.ms.test.core.domain.model;

import java.util.List;

public record TestCoverageRecord(String testId, java.util.List<CoverageLine> coverage) {
}
