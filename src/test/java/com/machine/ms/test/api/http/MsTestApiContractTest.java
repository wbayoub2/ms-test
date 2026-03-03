package com.machine.ms.test.api.http;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MsTestApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldIngestOpenClover() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/openclover/testwise-coverage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openCloverPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("OPEN_CLOVER"))
                .andExpect(jsonPath("$.records").value(1));
    }

    @Test
    void shouldIngestReportPortal() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/reportportal/test-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportPortalPayload("run_789", "PASS", "PASSED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("REPORT_PORTAL"))
                .andExpect(jsonPath("$.records").value(1));
    }

    @Test
    void shouldReturnDiff() throws Exception {
        mockMvc.perform(post("/api/v1/steps/diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stepPayload("req-diff", "ab12cd34", "ff89aa10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diffId").isNotEmpty())
                .andExpect(jsonPath("$.filesChanged").value(2));
    }

    @Test
    void shouldReturnImpactedTestsFromCoverage() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/openclover/testwise-coverage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openCloverPayload()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/steps/impacted-tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stepPayload("req-impact", "ab12cd34", "ff89aa10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impactId").isNotEmpty())
                .andExpect(jsonPath("$.tests[0].testId").isNotEmpty());
    }

    @Test
    void shouldEvaluateGateAndReplayByTuple() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/reportportal/test-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportPortalPayload("run_789", "PASS", "PASSED")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/steps/gates/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gatePayload("req-gate-a", "run_789", "validation-policy-v1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PASS"))
                .andExpect(jsonPath("$.idempotentReplay").value(false));

        mockMvc.perform(post("/api/v1/steps/gates/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gatePayload("req-gate-b", "run_789", "validation-policy-v1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PASS"))
                .andExpect(jsonPath("$.idempotentReplay").value(true));
    }

    @Test
    void shouldReturnConflictWhenRequestIdBoundToAnotherTuple() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/reportportal/test-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportPortalPayload("run_conflict", "PASS", "PASSED")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/steps/gates/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gatePayload("req-conflict", "run_conflict", "policy-1")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/steps/gates/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gatePayload("req-conflict", "run_conflict", "policy-2")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("GATE_IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void shouldRejectInvalidSha() throws Exception {
        mockMvc.perform(post("/api/v1/steps/diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stepPayload("req-bad", "badsha", "ff89aa10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    private String stepPayload(String requestId, String baseSha, String headSha) {
        return """
                {
                  "stepId":"step-1959-03",
                  "phase":"REVIEW",
                  "baseSha":"%s",
                  "headSha":"%s",
                  "requestId":"%s"
                }
                """.formatted(baseSha, headSha, requestId);
    }

    private String gatePayload(String requestId, String runId, String policyId) {
        return """
                {
                  "stepId":"step-1959-03",
                  "phase":"REVIEW",
                  "baseSha":"ab12cd34",
                  "headSha":"ff89aa10",
                  "requestId":"%s",
                  "policyId":"%s",
                  "runId":"%s"
                }
                """.formatted(requestId, policyId, runId);
    }

    private String openCloverPayload() {
        return """
                {
                  "requestId":"req-clover",
                  "service":"ms-plan",
                  "repository":"github.com/org/ms-plan",
                  "branch":"main",
                  "commitSha":"ff89aa10",
                  "tests":[{
                    "testId":"com.machine.ms.FooServiceTest#shouldCompute",
                    "coverage":[{
                      "filePath":"src/main/java/com/machine/ms/ab12cd3/ChangedService.java",
                      "className":"ChangedService",
                      "methodSig":"compute()",
                      "lineNo":12,
                      "covered":true
                    }]
                  }]
                }
                """;
    }

    private String reportPortalPayload(String runId, String status, String testStatus) {
        return """
                {
                  "requestId":"req-rp-%s",
                  "service":"ms-plan",
                  "branch":"main",
                  "commitSha":"ff89aa10",
                  "runId":"%s",
                  "launchId":"launch_001",
                  "status":"%s",
                  "results":[{
                    "testId":"com.machine.ms.FooServiceTest#shouldCompute",
                    "status":"%s",
                    "durationMs":128,
                    "errorSignature":null
                  }]
                }
                """.formatted(runId, runId, status, testStatus);
    }
}
