package com.machine.ms.test.core.service;

import com.machine.ms.test.core.domain.error.MsTestErrorCode;
import com.machine.ms.test.core.domain.error.MsTestException;
import com.machine.ms.test.core.domain.model.DiffSnapshot;
import com.machine.ms.test.core.domain.model.StepKey;
import com.machine.ms.test.core.port.out.CommitChangeStore;
import com.machine.ms.test.core.port.out.CoverageStore;
import com.machine.ms.test.core.port.out.DiffSnapshotStore;
import com.machine.ms.test.infra.config.MsTestStepValidationProperties;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DiffService {

    private final DiffSnapshotStore diffSnapshotStore;
    private final CoverageStore coverageStore;
    private final CommitChangeStore commitChangeStore;
    private final StepIdGenerator idGenerator;
    private final MsTestStepValidationProperties.DiffPolicy diffPolicy;

    public DiffService(DiffSnapshotStore diffSnapshotStore,
                       CoverageStore coverageStore,
                       CommitChangeStore commitChangeStore,
                       StepIdGenerator idGenerator,
                       MsTestStepValidationProperties.DiffPolicy diffPolicy) {
        this.diffSnapshotStore = diffSnapshotStore;
        this.coverageStore = coverageStore;
        this.commitChangeStore = commitChangeStore;
        this.idGenerator = idGenerator;
        this.diffPolicy = diffPolicy == null
                ? MsTestStepValidationProperties.DiffPolicy.ALLOW_SYNTHETIC_FALLBACK
                : diffPolicy;
    }

    public DiffSnapshot resolve(StepKey key, String requestId) {
        return diffSnapshotStore.findByStepKey(key)
                .orElseGet(() -> createAndStore(key, requestId));
    }

    private DiffSnapshot createAndStore(StepKey key, String requestId) {
        List<String> files = resolveRealDiff(key)
                .filter(changes -> !changes.isEmpty())
                .orElseGet(() -> fallbackOrThrow(key));
        DiffSnapshot snapshot = new DiffSnapshot(idGenerator.nextDiffId(), key, files, Instant.now(), requestId);
        return diffSnapshotStore.save(snapshot);
    }

    private Optional<List<String>> resolveRealDiff(StepKey key) {
        return coverageStore.findByCommitSha(key.headSha())
                .flatMap(snapshot -> commitChangeStore.findByHeadCommit(snapshot.service(), key.headSha()))
                .map(snapshot -> snapshot.changes().stream().map(change -> change.filePath()).distinct().toList());
    }

    private List<String> fallbackOrThrow(StepKey key) {
        if (diffPolicy == MsTestStepValidationProperties.DiffPolicy.ALLOW_SYNTHETIC_FALLBACK) {
            return syntheticDiff(key);
        }
        throw new MsTestException(
                MsTestErrorCode.DIFF_DATA_UNAVAILABLE,
                "strict diff policy requires real commit changes",
                Map.of(
                        "stepId", key.stepId(),
                        "phase", key.phase().name(),
                        "baseSha", key.baseSha(),
                        "headSha", key.headSha(),
                        "diffPolicy", diffPolicy.name()));
    }

    private List<String> syntheticDiff(StepKey key) {
        String prefixA = key.baseSha().substring(0, 7);
        String prefixB = key.headSha().substring(0, 7);
        return List.of(
                "src/main/java/com/machine/ms/" + prefixA + "/ChangedService.java",
                "src/test/java/com/machine/ms/" + prefixB + "/ChangedServiceTest.java");
    }
}
