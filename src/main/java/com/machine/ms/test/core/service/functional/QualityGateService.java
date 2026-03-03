package com.machine.ms.test.core.service.functional;

import com.machine.ms.test.api.contract.functional.QualityGateEvaluateRequest;

import java.util.List;
import java.util.Map;

public class QualityGateService {

    private final RunSnapshotQueries runQueries;
    private final ArtifactEvidenceRules artifactRules;

    public QualityGateService(RunSnapshotQueries runQueries, ArtifactEvidenceRules artifactRules) {
        this.runQueries = runQueries;
        this.artifactRules = artifactRules;
    }

    public Map<String, Object> evaluate(QualityGateEvaluateRequest request) {
        List<String> reasons = new java.util.ArrayList<>();
        if (request.policy().failOnCriticalTestRegression()) {
            boolean failedOnHead = runQueries.runByCommit(request.service(), request.branch(), request.headCommit())
                    .map(run -> run.failedCount() > 0)
                    .orElse(false);
            if (failedOnHead) {
                reasons.add("CRITICAL_TEST_REGRESSION");
            }
        }
        if (request.policy().failOnMissingEvidence() && !artifactRules.hasEvidenceForCommit(request.service(), request.headCommit())) {
            reasons.add("MISSING_EVIDENCE");
        }
        String decision = reasons.isEmpty() ? "PASS" : "FAIL";
        return Map.of("decision", decision, "reasons", reasons, "details", Map.of("service", request.service()));
    }

    public Map<String, Object> policies() {
        List<Map<String, Object>> list = List.of(
                Map.of("policyId", "validation-policy-v1", "description", "Regression + evidence policy"),
                Map.of("policyId", "validation-policy-strict", "description", "Strict fail on evidence and regression"));
        return Map.of("policies", list);
    }
}
