package com.machine.ms.test.core.service.functional;

import com.machine.ms.test.core.domain.error.MsTestErrorCode;
import com.machine.ms.test.core.domain.error.MsTestException;
import com.machine.ms.test.core.domain.model.TestRunSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TraceabilityService {

    private final RunSnapshotQueries runQueries;
    private final CoverageQueries coverageQueries;

    public TraceabilityService(RunSnapshotQueries runQueries, CoverageQueries coverageQueries) {
        this.runQueries = runQueries;
        this.coverageQueries = coverageQueries;
    }

    public Map<String, Object> history(String testId, String service, String branch, int limit) {
        List<Map<String, Object>> entries = runQueries.runs(service, branch).stream()
                .flatMap(run -> run.results().stream()
                        .filter(result -> result.testId().equals(testId))
                        .map(result -> mapHistory(run, result.status())))
                .limit(Math.max(1, limit))
                .toList();
        return Map.of("testId", testId, "service", service, "branch", branch, "history", entries);
    }

    public Map<String, Object> statusAt(String testId, String commitSha, String service) {
        var result = runQueries.allRuns().stream()
                .filter(run -> run.service().equals(service) && run.commitSha().equals(commitSha))
                .flatMap(run -> run.results().stream().filter(r -> r.testId().equals(testId))
                        .map(r -> Map.of("status", r.status(), "runId", run.runId(), "timestamp", run.ingestedAt())))
                .findFirst();
        return result.map(data -> Map.of("testId", testId, "commitSha", commitSha, "service", service, "data", data))
                .orElseThrow(() -> new MsTestException(MsTestErrorCode.TEST_NOT_FOUND, "test status not found at commit"));
    }

    public Map<String, Object> lastGreenFirstRed(String testId, String service, String branch) {
        List<Map<String, Object>> timeline = history(testId, service, branch, 1000).containsKey("history")
                ? castList(history(testId, service, branch, 1000).get("history"))
                : List.of();
        List<Map<String, Object>> asc = new ArrayList<>(timeline);
        java.util.Collections.reverse(asc);
        String lastGreen = null;
        String firstRed = null;
        for (Map<String, Object> row : asc) {
            String status = String.valueOf(row.get("status"));
            String commit = String.valueOf(row.get("commitSha"));
            if ("PASSED".equalsIgnoreCase(status) || "PASS".equalsIgnoreCase(status)) {
                lastGreen = commit;
            }
            if (("FAILED".equalsIgnoreCase(status) || "FAIL".equalsIgnoreCase(status)) && firstRed == null) {
                firstRed = commit;
                if (lastGreen != null) {
                    break;
                }
            }
        }
        return Map.of("testId", testId, "service", service, "branch", branch,
                "lastGreenCommit", lastGreen, "firstRedCommit", firstRed);
    }

    public Map<String, Object> compare(String testId, String service, String from, String to) {
        var fromStatus = runQueries.testResultAtCommit(service, "main", from, testId).map(r -> r.status()).orElse("UNKNOWN");
        var toStatus = runQueries.testResultAtCommit(service, "main", to, testId).map(r -> r.status()).orElse("UNKNOWN");
        Set<String> fromLines = coverageLines(from, testId);
        Set<String> toLines = coverageLines(to, testId);
        List<String> added = toLines.stream().filter(line -> !fromLines.contains(line)).toList();
        List<String> removed = fromLines.stream().filter(line -> !toLines.contains(line)).toList();
        Map<String, Object> fromData = Map.of("commitSha", from, "status", fromStatus, "coveredLines", fromLines.size());
        Map<String, Object> toData = Map.of("commitSha", to, "status", toStatus, "coveredLines", toLines.size());
        return Map.of("testId", testId, "service", service, "from", fromData, "to", toData,
                "coverageDelta", Map.of("added", added, "removed", removed));
    }

    public Map<String, Object> coverageAt(String testId, String commitSha, String service) {
        var record = coverageQueries.testAtCommit(commitSha, testId)
                .orElseThrow(() -> new MsTestException(MsTestErrorCode.TEST_NOT_FOUND, "test coverage not found at commit"));
        return Map.of("testId", testId, "service", service, "commitSha", commitSha, "coverage", record.coverage());
    }

    public Map<String, Object> coverageDiff(String testId, String service, String from, String to) {
        Set<String> fromLines = coverageLines(from, testId);
        Set<String> toLines = coverageLines(to, testId);
        List<String> added = toLines.stream().filter(line -> !fromLines.contains(line)).toList();
        List<String> removed = fromLines.stream().filter(line -> !toLines.contains(line)).toList();
        return Map.of("testId", testId, "service", service, "from", from, "to", to, "added", added, "removed", removed);
    }

    private Map<String, Object> mapHistory(TestRunSnapshot run, String status) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("commitSha", run.commitSha());
        row.put("runId", run.runId());
        row.put("status", status);
        row.put("timestamp", run.ingestedAt());
        return row;
    }

    private Set<String> coverageLines(String commitSha, String testId) {
        return coverageQueries.testAtCommit(commitSha, testId)
                .map(test -> test.coverage().stream().map(line -> line.filePath() + ":" + line.lineNo()).collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        return value == null ? List.of() : (List<Map<String, Object>>) value;
    }
}
