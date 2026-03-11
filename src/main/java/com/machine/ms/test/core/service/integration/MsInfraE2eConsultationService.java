package com.machine.ms.test.core.service.integration;

import com.machine.ms.test.api.contract.integration.MsInfraE2eConsultationResponse;
import com.machine.ms.test.core.domain.model.GateReport;
import com.machine.ms.test.core.domain.model.TestRunSnapshot;
import com.machine.ms.test.core.port.out.ArtifactLinkStore;
import com.machine.ms.test.core.port.out.GateReportStore;
import com.machine.ms.test.core.port.out.MsInfraToolPort;
import com.machine.ms.test.core.port.out.TestRunStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MsInfraE2eConsultationService {

    private final MsInfraToolPort msInfraToolPort;
    private final TestRunStore testRunStore;
    private final GateReportStore gateReportStore;
    private final ArtifactLinkStore artifactLinkStore;

    public MsInfraE2eConsultationService(MsInfraToolPort msInfraToolPort,
                                         TestRunStore testRunStore,
                                         GateReportStore gateReportStore,
                                         ArtifactLinkStore artifactLinkStore) {
        this.msInfraToolPort = msInfraToolPort;
        this.testRunStore = testRunStore;
        this.gateReportStore = gateReportStore;
        this.artifactLinkStore = artifactLinkStore;
    }

    public MsInfraE2eConsultationResponse consult(String runId, String correlationId) {
        Map<String, Object> upstream = fetchUpstream(runId, correlationId);
        Optional<TestRunSnapshot> localRun = testRunStore.findByRunId(runId);
        Optional<GateReport> gate = gateReportStore.findLatestByRunId(runId);
        List<String> impactedTests = localRun.map(run -> run.results().stream().map(result -> result.testId()).distinct().toList()).orElse(List.of());
        return new MsInfraE2eConsultationResponse(
                runId,
                stringValue(upstream.get("status"), localRun.map(TestRunSnapshot::status).orElse("UNKNOWN")),
                gate.map(report -> report.status().name()).orElse(localRun.map(run -> "PASS".equalsIgnoreCase(run.status()) ? "PASS" : "UNKNOWN").orElse("UNKNOWN")),
                stringValue(upstream.get("reportPortalLaunchId"), localRun.map(TestRunSnapshot::launchId).orElse(null)),
                stringValue(upstream.get("reportPortalUrl"), null),
                impactedTests,
                evidence(runId, upstream),
                upstream
        );
    }

    private Map<String, Object> fetchUpstream(String runId, String correlationId) {
        Map<String, Object> upstream = new LinkedHashMap<>();
        try {
            upstream.putAll(msInfraToolPort.getTestRun(runId, correlationId));
            Map<String, Object> artifacts = msInfraToolPort.getTestRunArtifacts(runId, correlationId);
            upstream.put("artifacts", artifacts.getOrDefault("artifacts", List.of()));
        } catch (RuntimeException ex) {
            upstream.put("status", "UNAVAILABLE");
            upstream.put("upstreamError", ex.getMessage());
            upstream.put("artifacts", List.of());
        }
        return upstream;
    }

    private List<Map<String, Object>> evidence(String runId, Map<String, Object> upstream) {
        List<Map<String, Object>> items = new ArrayList<>();
        artifactLinkStore.findByRunId(runId).ifPresent(snapshot -> snapshot.artifacts().forEach(artifact -> items.add(Map.of(
                "type", artifact.artifactType(),
                "storage", artifact.storage(),
                "url", artifact.url()
        ))));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> artifacts = (List<Map<String, Object>>) upstream.getOrDefault("artifacts", List.of());
        for (Map<String, Object> artifact : artifacts) {
            items.add(Map.of(
                    "type", "MS_INFRA_ARTIFACT",
                    "storage", "MS_INFRA",
                    "url", String.valueOf(artifact.getOrDefault("path", artifact.getOrDefault("name", "")))
            ));
        }
        return items;
    }

    private String stringValue(Object value, String fallback) {
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        return fallback;
    }
}
