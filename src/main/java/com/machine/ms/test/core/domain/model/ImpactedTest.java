package com.machine.ms.test.core.domain.model;

import java.util.List;

public record ImpactedTest(String testId, double confidence, List<String> reasons) {
}
