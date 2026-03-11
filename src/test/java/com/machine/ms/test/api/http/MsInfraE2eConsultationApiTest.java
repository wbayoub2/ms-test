package com.machine.ms.test.api.http;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MsInfraE2eConsultationApiTest {

    private static MockWebServer mockWebServer;

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void beforeAll() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void afterAll() throws Exception {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void dynamicProps(DynamicPropertyRegistry registry) {
        registry.add("mstest.msinfra.enabled", () -> true);
        registry.add("mstest.msinfra.base-url", () -> mockWebServer.url("/").toString());
    }

    @Test
    void shouldExposeAggregatedE2eConsultationView() throws Exception {
        mockWebServer.enqueue(jsonResponse("""
                {"runId":"run_consult","status":"COMPLETED","reportPortalLaunchId":"launch-123","reportPortalUrl":"http://rp.local/launch/123"}
                """, 200));
        mockWebServer.enqueue(jsonResponse("""
                {"runId":"run_consult","artifacts":[{"name":"summary.md","path":"/tmp/summary.md"}]}
                """, 200));

        mockMvc.perform(post("/api/v1/ingestion/reportportal/test-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportPortalPayload()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/ingestion/artifacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(artifactPayload()))
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

        mockMvc.perform(get("/api/v1/e2e/runs/run_consult"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value("run_consult"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.gateStatus").value("PASS"))
                .andExpect(jsonPath("$.reportPortalLaunchId").value("launch-123"))
                .andExpect(jsonPath("$.reportPortalUrl").value("http://rp.local/launch/123"))
                .andExpect(jsonPath("$.impactedTests[0]").value("com.machine.ms.FooServiceTest#shouldCompute"))
                .andExpect(jsonPath("$.evidence").isArray())
                .andExpect(jsonPath("$.upstream.status").value("COMPLETED"));
    }

    private MockResponse jsonResponse(String body, int status) {
        return new MockResponse().setResponseCode(status).addHeader("Content-Type", "application/json").setBody(body);
    }

    private String stepPayload(String requestId) {
        return """
                {"stepId":"step-1959-03","phase":"REVIEW","baseSha":"ab12cd34","headSha":"ff89aa10","requestId":"%s"}
                """.formatted(requestId);
    }

    private String gatePayload() {
        return """
                {"stepId":"step-1959-03","phase":"REVIEW","baseSha":"ab12cd34","headSha":"ff89aa10","requestId":"req-consult-gate","policyId":"validation-policy-v1","runId":"run_consult"}
                """;
    }

    private String reportPortalPayload() {
        return """
                {"requestId":"req-rp-consult","service":"ms-plan","branch":"main","commitSha":"ff89aa10","runId":"run_consult","launchId":"launch_001","status":"PASS","results":[{"testId":"com.machine.ms.FooServiceTest#shouldCompute","status":"PASSED","durationMs":128,"errorSignature":null}]}
                """;
    }

    private String artifactPayload() {
        return """
                {"requestId":"req-art-consult","runId":"run_consult","artifacts":[{"artifactType":"REPORTPORTAL_LAUNCH","storage":"REPORTPORTAL","url":"http://rp.local/launch/123","checksum":"launch-123"}]}
                """;
    }
}
