package com.machine.ms.test.core.service.functional;

import com.machine.ms.test.core.domain.error.MsTestErrorCode;
import com.machine.ms.test.core.domain.error.MsTestException;
import com.machine.ms.test.core.port.out.CommitChangeStore;

import java.util.List;
import java.util.Map;

public class ForensicsService {

    private final TraceabilityService traceabilityService;
    private final CommitChangeStore commitChangeStore;

    public ForensicsService(TraceabilityService traceabilityService, CommitChangeStore commitChangeStore) {
        this.traceabilityService = traceabilityService;
        this.commitChangeStore = commitChangeStore;
    }

    public Map<String, Object> regressionForensics(String testId, String service, String branch) {
        Map<String, Object> lgfr = traceabilityService.lastGreenFirstRed(testId, service, branch);
        String firstRed = (String) lgfr.get("firstRedCommit");
        if (firstRed == null) {
            throw new MsTestException(MsTestErrorCode.TEST_NOT_FOUND, "no failing commit found for test");
        }
        var suspects = suspects(firstRed, service, testId).get("suspects") instanceof List<?> rows
                ? (List<Map<String, ?>>) rows : List.of();
        return Map.of(
                "testId", testId,
                "service", service,
                "branch", branch,
                "lastGreenCommit", lgfr.get("lastGreenCommit"),
                "firstRedCommit", firstRed,
                "suspects", suspects);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> suspects(String commitSha, String service, String testId) {
        var snapshot = commitChangeStore.findByHeadCommit(service, commitSha)
                .orElseThrow(() -> new MsTestException(MsTestErrorCode.COMMIT_NOT_FOUND, "commit changes not found"));
        var ranked = snapshot.changes().stream()
                .map(change -> Map.of(
                        "filePath", change.filePath(),
                        "methodSig", change.methodSig(),
                        "score", 0.9,
                        "testId", testId))
                .toList();
        return Map.of("service", service, "commitSha", commitSha, "testId", testId, "suspects", ranked);
    }
}
