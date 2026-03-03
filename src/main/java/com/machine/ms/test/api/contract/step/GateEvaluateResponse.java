package com.machine.ms.test.api.contract.step;

import com.machine.ms.test.core.domain.model.GateStatus;
import com.machine.ms.test.core.domain.model.StepPhase;

import java.time.Instant;
import java.util.List;

public record GateEvaluateResponse(String requestId,
                                   String stepId,
                                   StepPhase phase,
                                   String baseSha,
                                   String headSha,
                                   String policyId,
                                   String runId,
                                   String gateReportId,
                                   GateStatus status,
                                   List<String> reasons,
                                   Instant evaluatedAt,
                                   boolean idempotentReplay) {
}
