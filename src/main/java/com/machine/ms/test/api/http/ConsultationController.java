package com.machine.ms.test.api.http;

import com.machine.ms.test.api.contract.consultation.CoverageConsultationResponse;
import com.machine.ms.test.api.contract.consultation.RunConsultationResponse;
import com.machine.ms.test.api.contract.step.DiffResponse;
import com.machine.ms.test.api.contract.step.GateEvaluateResponse;
import com.machine.ms.test.api.contract.step.ImpactedTestsResponse;
import com.machine.ms.test.core.domain.model.StepPhase;
import com.machine.ms.test.core.port.in.ConsultationUseCase;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/consultation")
public class ConsultationController {

    private final ConsultationUseCase consultationUseCase;

    public ConsultationController(ConsultationUseCase consultationUseCase) {
        this.consultationUseCase = consultationUseCase;
    }

    @GetMapping("/steps/diff")
    public DiffResponse getDiff(@RequestParam String stepId,
                                @RequestParam StepPhase phase,
                                @RequestParam String baseSha,
                                @RequestParam String headSha) {
        return consultationUseCase.getDiff(stepId, phase, baseSha, headSha);
    }

    @GetMapping("/steps/impacted-tests")
    public ImpactedTestsResponse getImpacted(@RequestParam String stepId,
                                             @RequestParam StepPhase phase,
                                             @RequestParam String baseSha,
                                             @RequestParam String headSha) {
        return consultationUseCase.getImpactedTests(stepId, phase, baseSha, headSha);
    }

    @GetMapping("/steps/gates/evaluate")
    public GateEvaluateResponse getGate(@RequestParam String stepId,
                                        @RequestParam StepPhase phase,
                                        @RequestParam String baseSha,
                                        @RequestParam String headSha,
                                        @RequestParam String policyId,
                                        @RequestParam String runId) {
        return consultationUseCase.getGate(stepId, phase, baseSha, headSha, policyId, runId);
    }

    @GetMapping("/coverage/{commitSha}")
    public CoverageConsultationResponse getCoverage(@PathVariable String commitSha) {
        return consultationUseCase.getCoverageByCommit(commitSha);
    }

    @GetMapping("/runs/{runId}")
    public RunConsultationResponse getRun(@PathVariable String runId) {
        return consultationUseCase.getRunById(runId);
    }
}
