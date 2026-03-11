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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MsInfraIntegrationApiTest {

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
        registry.add("mstest.msinfra.cookie", () -> "SESSION=test-cookie");
        registry.add("mstest.msinfra.authorization", () -> "Bearer test-token");
        registry.add("mstest.msinfra.session-id", () -> "session-e2e");
        registry.add("mstest.msinfra.max-attempts", () -> 2);
        registry.add("mstest.msinfra.retry-delay-ms", () -> 10);
        registry.add("mstest.msinfra.connect-timeout-ms", () -> 1000);
        registry.add("mstest.msinfra.read-timeout-ms", () -> 1000);
    }

    @Test
    void shouldStartAndPollWithCorrelationAndCookie() throws Exception {
        mockWebServer.enqueue(jsonResponse("{\"runId\":\"run-123\",\"status\":\"ACCEPTED\"}", 202));
        mockWebServer.enqueue(jsonResponse("{\"runId\":\"run-123\",\"status\":\"RUNNING\"}", 200));
        mockWebServer.enqueue(jsonResponse("{\"runId\":\"run-123\",\"status\":\"COMPLETED\"}", 200));

        mockMvc.perform(post("/api/v1/integration/ms-infra/test-run/start-and-poll")
                        .header("X-Request-Id", "rid-123")
                        .contentType(APPLICATION_JSON)
                .content("""
                                {"workflowId":"quality.yml","inputs":{"selectionMode":"IMPACTED_ONLY","tests":["A"]},"pollAttempts":2,"pollSleepMs":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value("run-123"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        RecordedRequest first = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        RecordedRequest second = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        RecordedRequest third = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertStartRequest(first);
        assertGetRequest(second, "/api/v1/e2e/runs/run-123");
        assertGetRequest(third, "/api/v1/e2e/runs/run-123");
    }

    @Test
    void shouldRetryOnceOnTransientFailure() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("{\"error\":\"upstream\"}"));
        mockWebServer.enqueue(jsonResponse("{\"runId\":\"run-456\",\"status\":\"ACCEPTED\"}", 202));
        mockWebServer.enqueue(jsonResponse("{\"runId\":\"run-456\",\"status\":\"COMPLETED\"}", 200));

        mockMvc.perform(post("/api/v1/integration/ms-infra/test-run/start-and-poll")
                        .contentType(APPLICATION_JSON)
                .content("""
                                {"workflowId":"quality.yml","inputs":{"selectionMode":"IMPACTED_ONLY"},"pollAttempts":1,"pollSleepMs":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value("run-456"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        RecordedRequest first = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        RecordedRequest second = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        RecordedRequest third = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(third).isNotNull();
    }

    private MockResponse jsonResponse(String body, int status) {
        return new MockResponse()
                .setResponseCode(status)
                .addHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private void assertStartRequest(RecordedRequest request) {
        assertThat(request).isNotNull();
        assertThat(request.getPath()).isEqualTo("/api/v1/e2e/runs");
        assertThat(request.getHeader("Cookie")).contains("SESSION=test-cookie");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-token");
        assertThat(request.getHeader("X-Session-Id")).isEqualTo("session-e2e");
        String outboundRequestId = request.getHeader("X-Request-Id");
        String body = request.getBody().readUtf8();
        assertThat(outboundRequestId).isNotBlank();
        assertThat(body).contains("\"target\":\"full-chain\"");
        assertThat(body).contains("\"mode\":\"mock\"");
    }

    private void assertGetRequest(RecordedRequest request, String expectedPath) {
        assertThat(request).isNotNull();
        assertThat(request.getPath()).isEqualTo(expectedPath);
        assertThat(request.getHeader("Cookie")).contains("SESSION=test-cookie");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-token");
        assertThat(request.getHeader("X-Session-Id")).isEqualTo("session-e2e");
        assertThat(request.getHeader("X-Request-Id")).isNotBlank();
    }
}
