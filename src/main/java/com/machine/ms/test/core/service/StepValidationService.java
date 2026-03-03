package com.machine.ms.test.core.service;

import com.machine.ms.test.api.contract.step.DiffResponse;
import com.machine.ms.test.api.contract.step.GateEvaluateRequest;
import com.machine.ms.test.api.contract.step.GateEvaluateResponse;
import com.machine.ms.test.api.contract.step.ImpactedTestResponseItem;
import com.machine.ms.test.api.contract.step.ImpactedTestsResponse;
import com.machine.ms.test.api.contract.step.StepContextRequest;
import com.machine.ms.test.core.domain.model.GateTupleKey;
import com.machine.ms.test.core.domain.model.StepKey;
import com.machine.ms.test.core.port.in.StepValidationUseCase;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StepValidationService implements StepValidationUseCase {

    private final StepRequestValidator validator;
    private final DiffService diffService;
    private final ImpactAnalysisService impactAnalysisService;
    private final GateEvaluationService gateEvaluationService;

    public StepValidationService(StepRequestValidator validator,
                                 DiffService diffService,
                                 ImpactAnalysisService impactAnalysisService,
                                 GateEvaluationService gateEvaluationService) {
        this.validator = validator;
        this.diffService = diffService;
        this.impactAnalysisService = impactAnalysisService;
        this.gateEvaluationService = gateEvaluationService;
    }

    @Override
    public DiffResponse getDiff(StepContextRequest request) {
        validator.validateContext(request);
        StepKey key = new StepKey(request.stepId(), request.phase(), request.baseSha(), request.headSha());
        var snapshot = diffService.resolve(key, request.requestId());
        return new DiffResponse(
                request.requestId(), request.stepId(), request.phase(), request.baseSha(), request.headSha(),
                snapshot.diffId(), snapshot.changedFiles().size(), snapshot.changedFiles(), snapshot.generatedAt());
    }

    @Override
    public ImpactedTestsResponse getImpactedTests(StepContextRequest request) {
        validator.validateContext(request);
        StepKey key = new StepKey(request.stepId(), request.phase(), request.baseSha(), request.headSha());
        var snapshot = impactAnalysisService.resolve(key, request.requestId());
        List<ImpactedTestResponseItem> tests = snapshot.tests().stream()
                .map(test -> new ImpactedTestResponseItem(test.testId(), test.confidence(), test.reasons()))
                .toList();
        return new ImpactedTestsResponse(
                request.requestId(), request.stepId(), request.phase(), request.baseSha(), request.headSha(),
                snapshot.impactId(), tests, snapshot.generatedAt());
    }

    @Override
    public GateEvaluateResponse evaluateGate(GateEvaluateRequest request) {
        validator.validateGate(request);
        GateTupleKey key = new GateTupleKey(
                request.stepId(), request.phase(), request.baseSha(), request.headSha(), request.policyId(), request.runId());
        GateResult gate = gateEvaluationService.evaluate(request.requestId(), key);
        var report = gate.report();
        return new GateEvaluateResponse(
                request.requestId(), request.stepId(), request.phase(), request.baseSha(), request.headSha(),
                request.policyId(), request.runId(), report.gateReportId(), report.status(), report.reasons(),
                report.evaluatedAt(), gate.replay());
    }
}
