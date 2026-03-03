package com.machine.ms.test.api.http.v1;

import com.machine.ms.test.core.service.functional.EvidenceService;
import com.machine.ms.test.core.service.functional.TestCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class EvidenceController {

    private final EvidenceService evidenceService;
    private final TestCatalogService testCatalogService;

    public EvidenceController(EvidenceService evidenceService, TestCatalogService testCatalogService) {
        this.evidenceService = evidenceService;
        this.testCatalogService = testCatalogService;
    }

    @GetMapping("/runs/{runId}/evidence")
    public Map<String, Object> runEvidence(@PathVariable String runId) {
        return evidenceService.runEvidence(runId);
    }

    @GetMapping("/tests/{testId}/evidence-at/{commitSha}")
    public Map<String, Object> testEvidence(@PathVariable String testId,
                                            @PathVariable String commitSha,
                                            @RequestParam String service) {
        return evidenceService.testEvidenceAt(testId, commitSha, service);
    }

    @GetMapping("/runs/search")
    public Map<String, Object> runsSearch(@RequestParam String service,
                                          @RequestParam(defaultValue = "main") String branch,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(defaultValue = "100") int limit) {
        return evidenceService.runsSearch(service, branch, status, limit);
    }

    @GetMapping("/tests/search")
    public Map<String, Object> testsSearch(@RequestParam String service,
                                           @RequestParam(required = false) String q,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(defaultValue = "100") int limit) {
        return testCatalogService.testsSearch(service, q, status, limit);
    }

    @GetMapping("/commits/{commitSha}/matrix")
    public Map<String, Object> commitMatrix(@PathVariable String commitSha,
                                            @RequestParam String service) {
        return evidenceService.commitMatrix(commitSha, service);
    }

    @GetMapping("/tests/flaky")
    public Map<String, Object> flaky(@RequestParam String service,
                                     @RequestParam(defaultValue = "main") String branch,
                                     @RequestParam(defaultValue = "50") int window,
                                     @RequestParam(defaultValue = "3") int minTransitions) {
        return testCatalogService.flaky(service, branch, window, minTransitions);
    }
}
