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
class MsTestToolBridgeApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeCatalogWithExactTools() throws Exception {
        mockMvc.perform(get("/tools/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.toolName=='step_execution_diff_get')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.toolName=='step_review_diff_get')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.toolName=='step_execution_impacted_tests_get')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.toolName=='step_review_impacted_tests_get')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.toolName=='step_gate_evaluate')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.toolName=='step_review_gate_evaluate')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.toolName=='step_execution_gate_evaluate')]").isNotEmpty())
                .andExpect(jsonPath("$[0].metadata.source").value("ms-test"))
                .andExpect(jsonPath("$[0].metadata.version").value("v1"));
    }

    @Test
    void shouldRouteDiffAndImpactedTools() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/commit-changes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commitChangesPayload()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/ingestion/openclover/testwise-coverage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openCloverPayload()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/tools/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toolCall("step_execution_diff_get", stepContextParams("EXECUTION"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diffId").isNotEmpty())
                .andExpect(jsonPath("$.changedFiles").value(1))
                .andExpect(jsonPath("$.changedFilePaths[0]").value("src/main/java/com/machine/ms/ab12cd3/ChangedService.java"));

        mockMvc.perform(post("/tools/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toolCall("step_review_diff_get", stepContextParams("REVIEW"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diffId").isNotEmpty())
                .andExpect(jsonPath("$.changedFilePaths[0]").value("src/main/java/com/machine/ms/ab12cd3/ChangedService.java"));

        mockMvc.perform(post("/tools/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toolCall("step_execution_impacted_tests_get", stepContextParams("EXECUTION"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impactedTestsCount").isNumber())
                .andExpect(jsonPath("$.impactedCalcLatencyMs").isNumber());

        mockMvc.perform(post("/tools/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toolCall("step_review_impacted_tests_get", stepContextParams("REVIEW"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impactedTestsCount").isNumber());
    }

    @Test
    void shouldRouteGateToolsAndExposeGateFields() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/reportportal/test-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportPortalPayload("run_bridge", "PASS", "PASSED")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/tools/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toolCall("step_gate_evaluate", gateParams("REVIEW", "run_bridge", "validation-policy-v1", "gate-req-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gateStatus").value("PASS"))
                .andExpect(jsonPath("$.gateReportId").isNotEmpty());

        mockMvc.perform(post("/tools/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toolCall("step_review_gate_evaluate", gateParams("REVIEW", "run_bridge", "validation-policy-v1", "gate-req-2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gateStatus").value("PASS"))
                .andExpect(jsonPath("$.gateReportId").isNotEmpty());

        mockMvc.perform(post("/tools/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toolCall("step_execution_gate_evaluate", gateParams("EXECUTION", "run_bridge", "validation-policy-v1", "gate-req-3"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gateStatus").value("PASS"))
                .andExpect(jsonPath("$.gateReportId").isNotEmpty());
    }

    @Test
    void shouldReturnCanonicalErrors() throws Exception {
        mockMvc.perform(post("/tools/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toolCall("unknown_tool", stepContextParams("EXECUTION"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETERS"))
                .andExpect(jsonPath("$.errorType").value("VALIDATION"))
                .andExpect(jsonPath("$.retriable").value(false))
                .andExpect(jsonPath("$.provider").value("MS_TEST"))
                .andExpect(jsonPath("$.context.param").value("toolId"));

        mockMvc.perform(post("/tools/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toolId":"step_execution_diff_get","params":{"stepId":"step-1959"}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETERS"))
                .andExpect(jsonPath("$.context.param").value("baseSha"));
    }

    @Test
    void shouldAcceptBridgeShaSampleAndGenerateRequestIdWhenMissing() throws Exception {
        mockMvc.perform(post("/tools/call")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {"toolId":"step_execution_diff_get","params":{"stepId":"step-1959","phase":"EXECUTION","baseSha":"abc","headSha":"def"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diffId").isNotEmpty())
                .andExpect(jsonPath("$.changedFiles").isNumber())
                .andExpect(jsonPath("$.changedFilePaths").isArray());
    }

    @Test
    void shouldKeepLegacyStepEndpointsWorking() throws Exception {
        mockMvc.perform(post("/api/v1/steps/diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stepId":"step-1959","phase":"REVIEW","baseSha":"ab12cd34","headSha":"ff89aa10","requestId":"legacy-req"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diffId").isNotEmpty());
    }

    private String toolCall(String toolId, String paramsJson) {
        return """
                {"requestId":"top-corr-id","toolId":"%s","params":%s}
                """.formatted(toolId, paramsJson);
    }

    private String stepContextParams(String phase) {
        return """
                {"planId":1959,"stepId":"step-1959","phase":"%s","baseSha":"ab12cd34","headSha":"ff89aa10","requestId":"ctx-req"}
                """.formatted(phase);
    }

    private String gateParams(String phase, String runId, String policyId, String requestId) {
        return """
                {"planId":1959,"stepId":"step-1959","phase":"%s","baseSha":"ab12cd34","headSha":"ff89aa10","runId":"%s","policyId":"%s","requestId":"%s"}
                """.formatted(phase, runId, policyId, requestId);
    }

    private String openCloverPayload() {
        return """
                {"requestId":"bridge-clover","service":"ms-plan","repository":"github.com/org/ms-plan","branch":"main","commitSha":"ff89aa10","tests":[{"testId":"com.machine.ms.FooServiceTest#shouldCompute","coverage":[{"filePath":"src/main/java/com/machine/ms/ab12cd3/ChangedService.java","className":"ChangedService","methodSig":"compute()","lineNo":12,"covered":true}]}]}
                """;
    }

    private String commitChangesPayload() {
        return """
                {"requestId":"bridge-changes","service":"ms-plan","branch":"main","baseCommit":"ab12cd34","headCommit":"ff89aa10","changes":[{"filePath":"src/main/java/com/machine/ms/ab12cd3/ChangedService.java","methodSig":"compute()","lineStart":12,"lineEnd":12,"changeType":"MODIFIED"}]}
                """;
    }

    private String reportPortalPayload(String runId, String status, String testStatus) {
        return """
                {"requestId":"bridge-rp-%s","service":"ms-plan","branch":"main","commitSha":"ff89aa10","runId":"%s","launchId":"launch_001","status":"%s","results":[{"testId":"com.machine.ms.FooServiceTest#shouldCompute","status":"%s","durationMs":128,"errorSignature":null}]}
                """.formatted(runId, runId, status, testStatus);
    }
}
