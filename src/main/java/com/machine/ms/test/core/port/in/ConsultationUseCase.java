package com.machine.ms.test.core.port.in;

import com.machine.ms.test.api.contract.consultation.CoverageConsultationResponse;
import com.machine.ms.test.api.contract.consultation.RunConsultationResponse;
import com.machine.ms.test.api.contract.step.DiffResponse;
import com.machine.ms.test.api.contract.step.GateEvaluateResponse;
import com.machine.ms.test.api.contract.step.ImpactedTestsResponse;
import com.machine.ms.test.core.domain.model.StepPhase;

public interface ConsultationUseCase {

    DiffResponse getDiff(String stepId, StepPhase phase, String baseSha, String headSha);

    ImpactedTestsResponse getImpactedTests(String stepId, StepPhase phase, String baseSha, String headSha);

    GateEvaluateResponse getGate(String stepId, StepPhase phase, String baseSha, String headSha, String policyId, String runId);

    CoverageConsultationResponse getCoverageByCommit(String commitSha);

    RunConsultationResponse getRunById(String runId);
}
