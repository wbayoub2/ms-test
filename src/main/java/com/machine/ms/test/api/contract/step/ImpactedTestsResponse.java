package com.machine.ms.test.api.contract.step;

import com.machine.ms.test.core.domain.model.StepPhase;

import java.time.Instant;
import java.util.List;

public record ImpactedTestsResponse(String requestId,
                                    String stepId,
                                    StepPhase phase,
                                    String baseSha,
                                    String headSha,
                                    String impactId,
                                    List<ImpactedTestResponseItem> tests,
                                    Instant generatedAt) {
}
