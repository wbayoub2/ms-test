package com.machine.ms.test.core.port.in;

import com.machine.ms.test.api.contract.step.DiffResponse;
import com.machine.ms.test.api.contract.step.GateEvaluateRequest;
import com.machine.ms.test.api.contract.step.GateEvaluateResponse;
import com.machine.ms.test.api.contract.step.ImpactedTestsResponse;
import com.machine.ms.test.api.contract.step.StepContextRequest;

public interface StepValidationUseCase {
    DiffResponse getDiff(StepContextRequest request);

    ImpactedTestsResponse getImpactedTests(StepContextRequest request);

    GateEvaluateResponse evaluateGate(GateEvaluateRequest request);
}
