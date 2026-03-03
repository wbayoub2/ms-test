package com.machine.ms.test.core.service;

import com.machine.ms.test.api.contract.step.GateEvaluateRequest;
import com.machine.ms.test.api.contract.step.StepContextRequest;
import com.machine.ms.test.core.domain.error.MsTestErrorCode;
import com.machine.ms.test.core.domain.error.MsTestException;

public class StepRequestValidator {

    public void validateContext(StepContextRequest request) {
        if (request.baseSha().equals(request.headSha())) {
            throw new MsTestException(MsTestErrorCode.INVALID_INPUT, "baseSha must be different from headSha");
        }
    }

    public void validateGate(GateEvaluateRequest request) {
        if (request.baseSha().equals(request.headSha())) {
            throw new MsTestException(MsTestErrorCode.INVALID_INPUT, "baseSha must be different from headSha");
        }
    }
}
