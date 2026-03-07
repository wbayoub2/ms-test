package com.machine.ms.test.api.http;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MsTestFunctionalTraceabilityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldServeTraceabilityAndImpactEndpoints() throws Exception {
        ingestCoverage("req-cov-a", "ab12cd34", true);
        ingestCoverage("req-cov-b", "ff89aa10", false);
        ingestRun("req-run-a", "ab12cd34", "run_a", "PASSED");
        ingestRun("req-run-b", "ff89aa10", "run_b", "FAILED");
        ingestChanges();

        mockMvc.perform(get("/api/v1/tests/com.machine.ms.FooServiceTest.shouldCompute/history")
                        .param("service", "ms-plan")
                        .param("branch", "main")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history").isArray());

        mockMvc.perform(get("/api/v1/tests/com.machine.ms.FooServiceTest.shouldCompute/status-at/ff89aa10")
                        .param("service", "ms-plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"));

        mockMvc.perform(get("/api/v1/tests/com.machine.ms.FooServiceTest.shouldCompute/last-green-first-red")
                        .param("service", "ms-plan")
                        .param("branch", "main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstRedCommit").value("ff89aa10"));

        mockMvc.perform(get("/api/v1/tests/com.machine.ms.FooServiceTest.shouldCompute/compare")
                        .param("service", "ms-plan")
                        .param("from", "ab12cd34")
                        .param("to", "ff89aa10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverageDelta").exists());

        mockMvc.perform(post("/api/v1/impact/compute-from-commits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"service":"ms-plan","baseCommit":"ab12cd34","headCommit":"ff89aa10","branch":"main"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impactedTests").isArray());

        mockMvc.perform(get("/api/v1/tests/com.machine.ms.FooServiceTest.shouldCompute/regression-forensics")
                        .param("service", "ms-plan")
                        .param("branch", "main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstRedCommit").value("ff89aa10"));
    }

    @Test
    void shouldKeepHistoryCardinalityStableForSameLogicalTestAcrossRuns() throws Exception {
        ingestRun("req-run-a", "ab12cd34", "run_hist_a", "PASSED");
        ingestRun("req-run-b", "ab12cd34", "run_hist_b", "FAILED");

        mockMvc.perform(get("/api/v1/tests/com.machine.ms.FooServiceTest.shouldCompute/history")
                        .param("service", "ms-plan")
                        .param("branch", "main")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history", hasSize(2)))
                .andExpect(jsonPath("$.history[0].runId").value("run_hist_b"));
    }

    private void ingestCoverage(String requestId, String commit, boolean covered) throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/openclover/testwise-coverage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestId":"%s","service":"ms-plan","repository":"github.com/org/ms-plan","branch":"main","commitSha":"%s",
                                  "tests":[{"testId":"com.machine.ms.FooServiceTest.shouldCompute","coverage":[{"filePath":"src/main/java/com/machine/ms/Foo.java","className":"Foo","methodSig":"compute()","lineNo":12,"covered":%s}]}]
                                }
                                """.formatted(requestId, commit, covered)))
                .andExpect(status().isOk());
    }

    private void ingestRun(String requestId, String commit, String runId, String testStatus) throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/reportportal/test-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestId":"%s","service":"ms-plan","branch":"main","commitSha":"%s","runId":"%s",
                                  "launchId":"launch-%s","status":"%s",
                                  "results":[{"testId":"com.machine.ms.FooServiceTest.shouldCompute","status":"%s","durationMs":100,"errorSignature":null}]
                                }
                                """.formatted(requestId, commit, runId, runId, testStatus, testStatus)))
                .andExpect(status().isOk());
    }

    private void ingestChanges() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/commit-changes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestId":"req-changes","service":"ms-plan","branch":"main","baseCommit":"ab12cd34","headCommit":"ff89aa10",
                                  "changes":[{"filePath":"src/main/java/com/machine/ms/Foo.java","methodSig":"compute()","lineStart":10,"lineEnd":40,"changeType":"MODIFIED"}]
                                }
                                """))
                .andExpect(status().isOk());
    }
}
