package com.machine.ms.test.core.service.functional;

import com.machine.ms.test.core.domain.error.MsTestErrorCode;
import com.machine.ms.test.core.domain.error.MsTestException;
import com.machine.ms.test.core.port.out.ArtifactLinkStore;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EvidenceService {

    private final RunSnapshotQueries runQueries;
    private final ArtifactLinkStore artifactLinkStore;

    public EvidenceService(RunSnapshotQueries runQueries, ArtifactLinkStore artifactLinkStore) {
        this.runQueries = runQueries;
        this.artifactLinkStore = artifactLinkStore;
    }

    public Map<String, Object> runEvidence(String runId) {
        var run = runQueries.allRuns().stream().filter(item -> item.runId().equals(runId)).findFirst()
                .orElseThrow(() -> new MsTestException(MsTestErrorCode.RUN_NOT_FOUND, "run not found"));
        var links = artifactLinkStore.findByRunId(runId)
                .map(snapshot -> snapshot.artifacts())
                .orElse(List.of());
        return Map.of("runId", runId, "status", run.status(), "evidence", links);
    }

    public Map<String, Object> testEvidenceAt(String testId, String commitSha, String service) {
        var run = runQueries.allRuns().stream()
                .filter(item -> item.service().equals(service) && item.commitSha().equals(commitSha))
                .filter(item -> item.results().stream().anyMatch(result -> result.testId().equals(testId)))
                .findFirst()
                .orElseThrow(() -> new MsTestException(MsTestErrorCode.EVIDENCE_NOT_FOUND, "test evidence not found"));
        return Map.of(
                "testId", testId,
                "commitSha", commitSha,
                "runId", run.runId(),
                "status", run.status(),
                "evidence", artifactLinkStore.findByRunId(run.runId()).map(s -> s.artifacts()).orElse(List.of()));
    }

    public Map<String, Object> runsSearch(String service, String branch, String status, int limit) {
        var runs = runQueries.runs(service, branch).stream()
                .filter(run -> status == null || status.isBlank() || run.status().equalsIgnoreCase(status))
                .limit(Math.max(1, limit))
                .map(run -> Map.of("runId", run.runId(), "commitSha", run.commitSha(), "status", run.status(), "ingestedAt", run.ingestedAt()))
                .toList();
        return Map.of("runs", runs, "service", service, "branch", branch);
    }

    public Map<String, Object> commitMatrix(String commitSha, String service) {
        var rows = runQueries.allRuns().stream()
                .filter(run -> run.service().equals(service) && run.commitSha().equals(commitSha))
                .flatMap(run -> run.results().stream().map(result -> matrixRow(run.runId(), result.testId(), result.status(), result.durationMs())))
                .toList();
        return Map.of("service", service, "commitSha", commitSha, "matrix", rows);
    }

    private Map<String, Object> matrixRow(String runId, String testId, String status, long durationMs) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("testId", testId);
        row.put("status", status);
        row.put("durationMs", durationMs);
        row.put("runId", runId);
        row.put("evidenceUrl", firstEvidenceUrl(runId));
        return row;
    }

    private String firstEvidenceUrl(String runId) {
        return artifactLinkStore.findByRunId(runId)
                .flatMap(snapshot -> snapshot.artifacts().stream().map(artifact -> artifact.url()).filter(Objects::nonNull).findFirst())
                .orElse(null);
    }
}
