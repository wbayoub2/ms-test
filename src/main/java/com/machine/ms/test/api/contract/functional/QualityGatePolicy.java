package com.machine.ms.test.api.contract.functional;

public record QualityGatePolicy(boolean failOnCriticalTestRegression,
                                boolean failOnMissingEvidence,
                                int maxNewUncoveredHotspots) {
}
