package com.machine.ms.test.api.contract.step;

import java.util.List;

public record ImpactedTestResponseItem(String testId, double confidence, List<String> reasons) {
}
