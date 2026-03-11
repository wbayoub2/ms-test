package com.machine.ms.test.api.http;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
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

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MsInfraE2eRunLinkingTest {

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
    void shouldLinkRunIdToGateEvidenceAndReportPortal() throws Exception {
        mockWebServer.enqueue(jsonResponse("{\"runId\":\"run_link\",\"status\":\"ACCEPTED\"}", 202));
        mockWebServer.enqueue(jsonResponse("{\"runId\":\"run_link\",\"status\":\"COMPLETED\",\"reportPortalLaunchId\":\"launch-link\",\"reportPortalUrl\":\"http://rp.local/launch/link\"}", 200));
        mockWebServer.enqueue(jsonResponse("{\"runId\":\"run_link\",\"status\":\"COMPLETED\",\"reportPortalLaunchId\":\"launch-link\",\"reportPortalUrl\":\"http://rp.local/launch/link\"}", 200));
        mockWebServer.enqueue(jsonResponse("{\"runId\":\"run_link\",\"artifacts\":[{\"name\":\"fingerprint.json\",\"path\":\"/tmp/fingerprint.json\"}]}", 200));

        mockMvc.perform(post("/api/v1/integration/ms-infra/test-run/start-and-poll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workflowId":"quality.yml","target":"bridge","mode":"real","runtimeMode":"shared","pollAttempts":2,"pollSleepMs":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value("run_link"))
                .andExpect(jsonPath("$.reportPortalUrl").value("http://rp.local/launch/link"));

        mockMvc.perform(post("/api/v1/ingestion/reportportal/test-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"req-link","service":"ms-plan","branch":"main","commitSha":"ff89aa10","runId":"run_link","launchId":"launch-link","status":"PASS","results":[{"testId":"com.machine.ms.FooServiceTest#shouldCompute","status":"PASSED","durationMs":128,"errorSignature":null}]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/steps/diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stepId":"step-link","phase":"EXECUTION","baseSha":"ab12cd34","headSha":"ff89aa10","requestId":"req-link-diff"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/steps/impacted-tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stepId":"step-link","phase":"EXECUTION","baseSha":"ab12cd34","headSha":"ff89aa10","requestId":"req-link-impact"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/steps/gates/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stepId":"step-link","phase":"EXECUTION","baseSha":"ab12cd34","headSha":"ff89aa10","requestId":"req-link-gate","policyId":"validation-policy-v1","runId":"run_link"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/e2e/runs/run_link"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value("run_link"))
                .andExpect(jsonPath("$.gateStatus").value("PASS"))
                .andExpect(jsonPath("$.reportPortalLaunchId").value("launch-link"))
                .andExpect(jsonPath("$.evidence[0].url").exists());

        RecordedRequest start = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertThat(start.getPath()).isEqualTo("/api/v1/e2e/runs");
        assertThat(start.getBody().readUtf8()).contains("\"target\":\"bridge\"");
    }

    private MockResponse jsonResponse(String body, int status) {
        return new MockResponse().setResponseCode(status).addHeader("Content-Type", "application/json").setBody(body);
    }
}
