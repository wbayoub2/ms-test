package com.machine.ms.test.api.http;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MsTestConsultationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReadCoverageAndRunFromConsultationEndpoints() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/openclover/testwise-coverage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openCloverPayload()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/ingestion/reportportal/test-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportPortalPayload()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/consultation/coverage/ff89aa10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commitSha").value("ff89aa10"))
                .andExpect(jsonPath("$.testCount").value(1));

        mockMvc.perform(get("/api/v1/consultation/runs/run_consult"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value("run_consult"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void shouldReadStepSnapshotsFromConsultationEndpoints() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/reportportal/test-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportPortalPayload()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/steps/diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stepPayload("req-consult-diff")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/steps/impacted-tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stepPayload("req-consult-impact")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/steps/gates/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gatePayload()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/consultation/steps/diff")
                        .param("stepId", "step-1959-03")
                        .param("phase", "REVIEW")
                        .param("baseSha", "ab12cd34")
                        .param("headSha", "ff89aa10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diffId").isNotEmpty());

        mockMvc.perform(get("/api/v1/consultation/steps/gates/evaluate")
                        .param("stepId", "step-1959-03")
                        .param("phase", "REVIEW")
                        .param("baseSha", "ab12cd34")
                        .param("headSha", "ff89aa10")
                        .param("policyId", "validation-policy-v1")
                        .param("runId", "run_consult"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PASS"));
    }

    private String stepPayload(String requestId) {
        return """
                {
                  "stepId":"step-1959-03",
                  "phase":"REVIEW",
                  "baseSha":"ab12cd34",
                  "headSha":"ff89aa10",
                  "requestId":"%s"
                }
                """.formatted(requestId);
    }

    private String gatePayload() {
        return """
                {
                  "stepId":"step-1959-03",
                  "phase":"REVIEW",
                  "baseSha":"ab12cd34",
                  "headSha":"ff89aa10",
                  "requestId":"req-consult-gate",
                  "policyId":"validation-policy-v1",
                  "runId":"run_consult"
                }
                """;
    }

    private String openCloverPayload() {
        return """
                {
                  "requestId":"req-clover-consult",
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

    private String reportPortalPayload() {
        return """
                {
                  "requestId":"req-rp-consult",
                  "service":"ms-plan",
                  "branch":"main",
                  "commitSha":"ff89aa10",
                  "runId":"run_consult",
                  "launchId":"launch_001",
                  "status":"PASS",
                  "results":[{
                    "testId":"com.machine.ms.FooServiceTest#shouldCompute",
                    "status":"PASSED",
                    "durationMs":128,
                    "errorSignature":null
                  }]
                }
                """;
    }
}
