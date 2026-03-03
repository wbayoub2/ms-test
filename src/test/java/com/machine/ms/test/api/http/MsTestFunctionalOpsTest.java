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
class MsTestFunctionalOpsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldServeEvidenceCoverageAndQualityEndpoints() throws Exception {
        ingestRun();
        ingestArtifacts();
        ingestCoverage();

        mockMvc.perform(get("/api/v1/runs/run_ops/evidence"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidence").isArray());

        mockMvc.perform(get("/api/v1/tests/com.machine.ms.FooServiceTest.shouldCompute/evidence-at/ff89aa10")
                        .param("service", "ms-plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value("run_ops"));

        mockMvc.perform(get("/api/v1/runs/search")
                        .param("service", "ms-plan")
                        .param("branch", "main")
                        .param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runs").isArray());

        mockMvc.perform(get("/api/v1/tests/search")
                        .param("service", "ms-plan")
                        .param("q", "FooServiceTest")
                        .param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray());

        mockMvc.perform(get("/api/v1/commits/ff89aa10/matrix")
                        .param("service", "ms-plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matrix").isArray());

        mockMvc.perform(get("/api/v1/coverage/uncovered")
                        .param("service", "ms-plan")
                        .param("branch", "main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uncovered").isArray());

        mockMvc.perform(post("/api/v1/quality-gates/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"service":"ms-plan","branch":"main","baseCommit":"ab12cd34","headCommit":"ff89aa10",
                                 "policy":{"failOnCriticalTestRegression":true,"failOnMissingEvidence":true,"maxNewUncoveredHotspots":3}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").exists());

        mockMvc.perform(get("/api/v1/quality-gates/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policies").isArray());
    }

    private void ingestRun() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/reportportal/test-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"req-run-ops","service":"ms-plan","branch":"main","commitSha":"ff89aa10","runId":"run_ops",
                                 "launchId":"launch_ops","status":"FAILED",
                                 "results":[{"testId":"com.machine.ms.FooServiceTest.shouldCompute","status":"FAILED","durationMs":120,"errorSignature":"assert"}]}
                                """))
                .andExpect(status().isOk());
    }

    private void ingestArtifacts() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/artifacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"req-art","runId":"run_ops","artifacts":[{"artifactType":"CLOVER_XML","storage":"MINIO","url":"s3://qa-evidence/run_ops/clover.xml","checksum":"abc"}]}
                                """))
                .andExpect(status().isOk());
    }

    private void ingestCoverage() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/openclover/testwise-coverage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"req-cov-ops","service":"ms-plan","repository":"github.com/org/ms-plan","branch":"main","commitSha":"ff89aa10",
                                 "tests":[{"testId":"com.machine.ms.FooServiceTest.shouldCompute","coverage":[{"filePath":"src/main/java/com/machine/ms/Foo.java","className":"Foo","methodSig":"compute()","lineNo":21,"covered":false}]}]}
                                """))
                .andExpect(status().isOk());
    }
}
