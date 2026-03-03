package com.machine.ms.test.core.service.functional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestCatalogService {

    private final RunSnapshotQueries runQueries;

    public TestCatalogService(RunSnapshotQueries runQueries) {
        this.runQueries = runQueries;
    }

    public Map<String, Object> testsSearch(String service, String query, String status, int limit) {
        var rows = runQueries.allRuns().stream()
                .filter(run -> run.service().equals(service))
                .flatMap(run -> run.results().stream()
                        .filter(result -> query == null || query.isBlank() || result.testId().contains(query))
                        .filter(result -> status == null || status.isBlank() || result.status().equalsIgnoreCase(status))
                        .map(result -> Map.of(
                                "testId", result.testId(),
                                "status", result.status(),
                                "runId", run.runId(),
                                "commitSha", run.commitSha())))
                .limit(Math.max(1, limit))
                .toList();
        return Map.of("service", service, "results", rows);
    }

    public Map<String, Object> flaky(String service, String branch, int window, int minTransitions) {
        List<Map<String, Object>> flaky = new ArrayList<>();
        var runs = runQueries.runs(service, branch).stream().limit(Math.max(2, window)).toList();
        var grouped = runs.stream().flatMap(run -> run.results().stream().map(result -> Map.entry(result.testId(), result.status())))
                .collect(java.util.stream.Collectors.groupingBy(Map.Entry::getKey,
                        java.util.stream.Collectors.mapping(Map.Entry::getValue, java.util.stream.Collectors.toList())));
        grouped.forEach((testId, statuses) -> {
            int transitions = countTransitions(statuses);
            if (transitions >= minTransitions) {
                flaky.add(Map.of("testId", testId, "transitions", transitions, "window", statuses.size()));
            }
        });
        return Map.of("service", service, "branch", branch, "flakyTests", flaky);
    }

    private int countTransitions(List<String> statuses) {
        int transitions = 0;
        for (int i = 1; i < statuses.size(); i++) {
            if (!statuses.get(i - 1).equalsIgnoreCase(statuses.get(i))) {
                transitions++;
            }
        }
        return transitions;
    }
}
