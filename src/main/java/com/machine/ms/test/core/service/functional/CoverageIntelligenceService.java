package com.machine.ms.test.core.service.functional;

import java.util.List;
import java.util.Map;

public class CoverageIntelligenceService {

    private final CoverageQueries coverageQueries;

    public CoverageIntelligenceService(CoverageQueries coverageQueries) {
        this.coverageQueries = coverageQueries;
    }

    public Map<String, Object> uncovered(String service, String branch, int limit) {
        var rows = coverageQueries.byServiceAndBranch(service, branch).stream()
                .flatMap(snapshot -> snapshot.tests().stream())
                .flatMap(test -> test.coverage().stream().filter(line -> !line.covered())
                        .map(line -> Map.of("testId", test.testId(), "filePath", line.filePath(), "lineNo", line.lineNo())))
                .limit(Math.max(1, limit))
                .toList();
        return Map.of("service", service, "branch", branch, "uncovered", rows);
    }

    public Map<String, Object> hotspots(String service, String branch, int limit) {
        var hotspots = uncovered(service, branch, limit * 4).get("uncovered") instanceof List<?> rows
                ? (List<Map<String, ?>>) rows : List.of();
        return Map.of("service", service, "branch", branch, "hotspots", hotspots.stream().limit(Math.max(1, limit)).toList());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> backlog(String service, String branch) {
        var list = (List<Map<String, ?>>) hotspots(service, branch, 20).get("hotspots");
        var backlog = list.stream()
                .map(item -> Map.of("priority", "HIGH", "filePath", item.get("filePath"), "lineNo", item.get("lineNo")))
                .toList();
        return Map.of("service", service, "branch", branch, "backlog", backlog);
    }

    public Map<String, Object> servicesOverview(String branch) {
        var rows = coverageQueries.all().stream()
                .filter(snapshot -> snapshot.branch().equals(branch))
                .map(snapshot -> Map.of(
                        "service", snapshot.service(),
                        "commitSha", snapshot.commitSha(),
                        "tests", snapshot.tests().size(),
                        "coveredLines", snapshot.tests().stream().flatMap(t -> t.coverage().stream()).filter(line -> line.covered()).count()))
                .toList();
        return Map.of("branch", branch, "services", rows);
    }
}
