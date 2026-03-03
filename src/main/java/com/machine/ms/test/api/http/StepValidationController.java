package com.machine.ms.test.api.http;

import com.machine.ms.test.api.contract.step.DiffResponse;
import com.machine.ms.test.api.contract.step.GateEvaluateRequest;
import com.machine.ms.test.api.contract.step.GateEvaluateResponse;
import com.machine.ms.test.api.contract.step.ImpactedTestsResponse;
import com.machine.ms.test.api.contract.step.StepContextRequest;
import com.machine.ms.test.core.port.in.StepValidationUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/steps")
public class StepValidationController {

    private final StepValidationUseCase stepValidationUseCase;

    public StepValidationController(StepValidationUseCase stepValidationUseCase) {
        this.stepValidationUseCase = stepValidationUseCase;
    }

    @PostMapping("/diff")
    public DiffResponse diff(@Valid @RequestBody StepContextRequest request) {
        return stepValidationUseCase.getDiff(request);
    }

    @PostMapping("/impacted-tests")
    public ImpactedTestsResponse impactedTests(@Valid @RequestBody StepContextRequest request) {
        return stepValidationUseCase.getImpactedTests(request);
    }

    @PostMapping("/gates/evaluate")
    public GateEvaluateResponse evaluateGate(@Valid @RequestBody GateEvaluateRequest request) {
        return stepValidationUseCase.evaluateGate(request);
    }
}
