package com.machine.ms.test.core.service;

import com.machine.ms.test.api.contract.consultation.CoverageConsultationResponse;
import com.machine.ms.test.api.contract.consultation.RunConsultationResponse;
import com.machine.ms.test.api.contract.step.DiffResponse;
import com.machine.ms.test.api.contract.step.GateEvaluateResponse;
import com.machine.ms.test.api.contract.step.ImpactedTestResponseItem;
import com.machine.ms.test.api.contract.step.ImpactedTestsResponse;
import com.machine.ms.test.core.domain.error.MsTestErrorCode;
import com.machine.ms.test.core.domain.error.MsTestException;
import com.machine.ms.test.core.domain.model.GateTupleKey;
import com.machine.ms.test.core.domain.model.StepKey;
import com.machine.ms.test.core.domain.model.StepPhase;
import com.machine.ms.test.core.port.in.ConsultationUseCase;
import com.machine.ms.test.core.port.out.CoverageStore;
import com.machine.ms.test.core.port.out.DiffSnapshotStore;
import com.machine.ms.test.core.port.out.GateReportStore;
import com.machine.ms.test.core.port.out.ImpactSnapshotStore;
import com.machine.ms.test.core.port.out.TestRunStore;
import org.springframework.stereotype.Service;

@Service
public class ConsultationService implements ConsultationUseCase {

    private final DiffSnapshotStore diffSnapshotStore;
    private final ImpactSnapshotStore impactSnapshotStore;
    private final GateReportStore gateReportStore;
    private final CoverageStore coverageStore;
    private final TestRunStore testRunStore;

    public ConsultationService(DiffSnapshotStore diffSnapshotStore,
                               ImpactSnapshotStore impactSnapshotStore,
                               GateReportStore gateReportStore,
                               CoverageStore coverageStore,
                               TestRunStore testRunStore) {
        this.diffSnapshotStore = diffSnapshotStore;
        this.impactSnapshotStore = impactSnapshotStore;
        this.gateReportStore = gateReportStore;
        this.coverageStore = coverageStore;
        this.testRunStore = testRunStore;
    }

    @Override
    public DiffResponse getDiff(String stepId, StepPhase phase, String baseSha, String headSha) {
        StepKey key = new StepKey(stepId, phase, baseSha, headSha);
        var snapshot = diffSnapshotStore.findByStepKey(key).orElseThrow(() -> notFound("diff snapshot"));
        return new DiffResponse(snapshot.requestId(), stepId, phase, baseSha, headSha,
                snapshot.diffId(), snapshot.changedFiles().size(), snapshot.changedFiles(), snapshot.generatedAt());
    }

    @Override
    public ImpactedTestsResponse getImpactedTests(String stepId, StepPhase phase, String baseSha, String headSha) {
        StepKey key = new StepKey(stepId, phase, baseSha, headSha);
        var snapshot = impactSnapshotStore.findByStepKey(key).orElseThrow(() -> notFound("impact snapshot"));
        var tests = snapshot.tests().stream()
                .map(test -> new ImpactedTestResponseItem(test.testId(), test.confidence(), test.reasons()))
                .toList();
        return new ImpactedTestsResponse(snapshot.requestId(), stepId, phase, baseSha, headSha,
                snapshot.impactId(), tests, snapshot.generatedAt());
    }

    @Override
    public GateEvaluateResponse getGate(String stepId, StepPhase phase, String baseSha, String headSha, String policyId, String runId) {
        GateTupleKey key = new GateTupleKey(stepId, phase, baseSha, headSha, policyId, runId);
        var report = gateReportStore.findByTupleKey(key).orElseThrow(() -> notFound("gate report"));
        return new GateEvaluateResponse(report.requestId(), stepId, phase, baseSha, headSha,
                policyId, runId, report.gateReportId(), report.status(), report.reasons(), report.evaluatedAt(), true);
    }

    @Override
    public CoverageConsultationResponse getCoverageByCommit(String commitSha) {
        var snapshot = coverageStore.findByCommitSha(commitSha).orElseThrow(() -> notFound("coverage snapshot"));
        return new CoverageConsultationResponse(snapshot.requestId(), snapshot.ingestionId(), snapshot.service(),
                snapshot.repository(), snapshot.branch(), snapshot.commitSha(), snapshot.tests().size(), snapshot.ingestedAt());
    }

    @Override
    public RunConsultationResponse getRunById(String runId) {
        var snapshot = testRunStore.findByRunId(runId).orElseThrow(() -> new MsTestException(MsTestErrorCode.RUN_NOT_FOUND, "runId not found"));
        int total = snapshot.results().size();
        int failed = (int) snapshot.failedCount();
        return new RunConsultationResponse(snapshot.requestId(), snapshot.ingestionId(), snapshot.service(),
                snapshot.branch(), snapshot.commitSha(), snapshot.runId(), snapshot.launchId(), snapshot.status(), total, failed, snapshot.ingestedAt());
    }

    private MsTestException notFound(String resource) {
        return new MsTestException(MsTestErrorCode.SNAPSHOT_NOT_FOUND, resource + " not found");
    }
}
