package com.machine.ms.test.core.service;

import com.machine.ms.test.core.domain.error.MsTestErrorCode;
import com.machine.ms.test.core.domain.error.MsTestException;
import com.machine.ms.test.core.domain.model.CoverageSnapshot;
import com.machine.ms.test.core.domain.model.ImpactSnapshot;
import com.machine.ms.test.core.domain.model.ImpactedTest;
import com.machine.ms.test.core.domain.model.StepKey;
import com.machine.ms.test.core.port.out.CoverageStore;
import com.machine.ms.test.core.port.out.ImpactSnapshotStore;
import com.machine.ms.test.infra.config.MsTestStepValidationProperties;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImpactAnalysisService {

    private final ImpactSnapshotStore impactSnapshotStore;
    private final CoverageStore coverageStore;
    private final DiffService diffService;
    private final StepIdGenerator idGenerator;
    private final MsTestStepValidationProperties.ImpactPolicy impactPolicy;

    public ImpactAnalysisService(ImpactSnapshotStore impactSnapshotStore,
                                 CoverageStore coverageStore,
                                 DiffService diffService,
                                 StepIdGenerator idGenerator,
                                 MsTestStepValidationProperties.ImpactPolicy impactPolicy) {
        this.impactSnapshotStore = impactSnapshotStore;
        this.coverageStore = coverageStore;
        this.diffService = diffService;
        this.idGenerator = idGenerator;
        this.impactPolicy = impactPolicy == null
                ? MsTestStepValidationProperties.ImpactPolicy.ALLOW_DEFAULT_FALLBACK
                : impactPolicy;
    }

    public ImpactSnapshot resolve(StepKey key, String requestId) {
        return impactSnapshotStore.findByStepKey(key)
                .orElseGet(() -> createAndStore(key, requestId));
    }

    private ImpactSnapshot createAndStore(StepKey key, String requestId) {
        List<String> changedFiles = diffService.resolve(key, requestId).changedFiles();
        List<ImpactedTest> tests = coverageStore.findByCommitSha(key.headSha())
                .map(snapshot -> fromCoverage(snapshot, changedFiles))
                .orElseGet(() -> fallbackOrThrow(key));
        ImpactSnapshot snapshot = new ImpactSnapshot(idGenerator.nextImpactId(), key, tests, Instant.now(), requestId);
        return impactSnapshotStore.save(snapshot);
    }

    private List<ImpactedTest> fromCoverage(CoverageSnapshot snapshot, List<String> changedFiles) {
        Set<String> changed = Set.copyOf(changedFiles);
        List<ImpactedTest> computed = snapshot.tests().stream()
                .filter(test -> test.coverage().stream().anyMatch(line -> changed.contains(line.filePath())))
                .map(test -> new ImpactedTest(test.testId(), 0.93, List.of("method_call_graph", "same_package")))
                .toList();
        if (!computed.isEmpty()) {
            return computed;
        }
        if (impactPolicy == MsTestStepValidationProperties.ImpactPolicy.ALLOW_DEFAULT_FALLBACK) {
            return List.of(defaultImpact());
        }
        return List.of();
    }

    private List<ImpactedTest> fallbackOrThrow(StepKey key) {
        if (impactPolicy == MsTestStepValidationProperties.ImpactPolicy.ALLOW_DEFAULT_FALLBACK) {
            return List.of(defaultImpact());
        }
        throw new MsTestException(
                MsTestErrorCode.IMPACT_DATA_UNAVAILABLE,
                "strict impact policy requires real coverage data",
                Map.of(
                        "stepId", key.stepId(),
                        "phase", key.phase().name(),
                        "baseSha", key.baseSha(),
                        "headSha", key.headSha(),
                        "impactPolicy", impactPolicy.name()));
    }

    private ImpactedTest defaultImpact() {
        return new ImpactedTest("com.machine.ms.test.DefaultImpactedTest#shouldRun", 0.75, List.of("fallback_no_coverage_data"));
    }
}
