package com.machine.ms.test.api.http;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MsTestPerformanceSmokeTest {

    private static final long MAX_ENDPOINT_DURATION_MS = 5000;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldKeepCriticalEndpointsUnderFiveSeconds() throws Exception {
        seedDataset();

        assertDuration(post("/api/v1/impact/compute-from-commits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"service":"ms-plan","baseCommit":"ab12cd34","headCommit":"ff89aa10","branch":"main"}
                        """));

        assertDuration(get("/api/v1/tests/com.machine.ms.FooServiceTest.shouldCompute/history")
                .param("service", "ms-plan")
                .param("branch", "main")
                .param("limit", "20"));

        assertDuration(post("/api/v1/quality-gates/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"service":"ms-plan","branch":"main","baseCommit":"ab12cd34","headCommit":"ff89aa10",
                        "policy":{"failOnCriticalTestRegression":true,"failOnMissingEvidence":true,"maxNewUncoveredHotspots":3}}
                        """));
    }

    private void assertDuration(MockHttpServletRequestBuilder request) throws Exception {
        long start = System.nanoTime();
        mockMvc.perform(request).andExpect(status().isOk());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsedMs).isLessThan(MAX_ENDPOINT_DURATION_MS);
    }

    private void seedDataset() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/openclover/testwise-coverage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"req-perf-cov-a","service":"ms-plan","repository":"github.com/org/ms-plan","branch":"main","commitSha":"ab12cd34","tests":[{"testId":"com.machine.ms.FooServiceTest.shouldCompute","coverage":[{"filePath":"src/main/java/com/machine/ms/Foo.java","className":"Foo","methodSig":"compute()","lineNo":12,"covered":true}]}]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/ingestion/openclover/testwise-coverage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"req-perf-cov-b","service":"ms-plan","repository":"github.com/org/ms-plan","branch":"main","commitSha":"ff89aa10","tests":[{"testId":"com.machine.ms.FooServiceTest.shouldCompute","coverage":[{"filePath":"src/main/java/com/machine/ms/Foo.java","className":"Foo","methodSig":"compute()","lineNo":12,"covered":false}]}]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/ingestion/reportportal/test-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"req-perf-run-a","service":"ms-plan","branch":"main","commitSha":"ab12cd34","runId":"run_perf_a","launchId":"launch_perf_a","status":"PASSED","results":[{"testId":"com.machine.ms.FooServiceTest.shouldCompute","status":"PASSED","durationMs":90,"errorSignature":null}]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/ingestion/reportportal/test-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"req-perf-run-b","service":"ms-plan","branch":"main","commitSha":"ff89aa10","runId":"run_perf_b","launchId":"launch_perf_b","status":"FAILED","results":[{"testId":"com.machine.ms.FooServiceTest.shouldCompute","status":"FAILED","durationMs":100,"errorSignature":"assert"}]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/ingestion/commit-changes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"req-perf-chg","service":"ms-plan","branch":"main","baseCommit":"ab12cd34","headCommit":"ff89aa10","changes":[{"filePath":"src/main/java/com/machine/ms/Foo.java","methodSig":"compute()","lineStart":10,"lineEnd":40,"changeType":"MODIFIED"}]}
                                """))
                .andExpect(status().isOk());
    }
}
