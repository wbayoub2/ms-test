package com.machine.ms.test.core.service.functional;

import com.machine.ms.test.api.contract.functional.ImpactComputeFromCommitsRequest;
import com.machine.ms.test.api.contract.functional.ImpactComputeRequest;
import com.machine.ms.test.core.domain.error.MsTestErrorCode;
import com.machine.ms.test.core.domain.error.MsTestException;
import com.machine.ms.test.core.domain.model.CommitChange;
import com.machine.ms.test.core.port.out.CommitChangeStore;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ImpactComputationService {

    private final CoverageQueries coverageQueries;
    private final CommitChangeStore commitChangeStore;

    public ImpactComputationService(CoverageQueries coverageQueries, CommitChangeStore commitChangeStore) {
        this.coverageQueries = coverageQueries;
        this.commitChangeStore = commitChangeStore;
    }

    public Map<String, Object> compute(ImpactComputeRequest request) {
        List<CommitChange> changes = request.changes().stream()
                .map(change -> new CommitChange(change.filePath(), "unknown", change.lineStart(), change.lineEnd(), "MODIFIED"))
                .toList();
        return buildImpact(request.service(), request.baseCommit(), request.headCommit(), changes);
    }

    public Map<String, Object> computeFromCommits(ImpactComputeFromCommitsRequest request) {
        List<CommitChange> changes = commitChangeStore.findByHeadCommit(request.service(), request.headCommit())
                .map(snapshot -> snapshot.changes())
                .orElseThrow(() -> new MsTestException(MsTestErrorCode.COMMIT_NOT_FOUND, "commit changes not found"));
        return buildImpact(request.service(), request.baseCommit(), request.headCommit(), changes);
    }

    private Map<String, Object> buildImpact(String service, String baseCommit, String headCommit, List<CommitChange> changes) {
        var coverage = coverageQueries.byCommit(headCommit)
                .orElseThrow(() -> new MsTestException(MsTestErrorCode.COMMIT_NOT_FOUND, "head commit coverage not found"));
        Set<String> changedFiles = changes.stream().map(CommitChange::filePath).collect(Collectors.toSet());
        List<Map<String, Object>> impacted = coverage.tests().stream()
                .filter(test -> test.coverage().stream().anyMatch(line -> changedFiles.contains(line.filePath())))
                .map(test -> Map.of(
                        "testId", test.testId(),
                        "testName", test.testId(),
                        "confidence", "HIGH",
                        "reasons", List.of("DIRECT_LINE_COVERAGE")))
                .toList();
        List<String> minimal = impacted.stream().map(item -> String.valueOf(item.get("testId"))).toList();
        return Map.of(
                "service", service,
                "baseCommit", baseCommit,
                "headCommit", headCommit,
                "changedFiles", changedFiles,
                "impactedTests", impacted,
                "minimalSafeSet", minimal);
    }
}
