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
class MsTestPlanToInfraToTestChainApiTest {

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
    void shouldExecutePlanToInfraAndTraceabilityChain() throws Exception {
        mockWebServer.enqueue(jsonResponse("{\"runId\":\"run_chain\",\"status\":\"RUNNING\"}", 200));
        mockWebServer.enqueue(jsonResponse("{\"runId\":\"run_chain\",\"status\":\"COMPLETED\"}", 200));

        mockMvc.perform(post("/api/v1/ingestion/openclover/testwise-coverage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openCloverPayload()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/ingestion/commit-changes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commitChangesPayload()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/tools/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toolCall("step_execution_impacted_tests_get", stepContextParams("EXECUTION"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impactId").isNotEmpty());

        mockMvc.perform(post("/api/v1/integration/ms-infra/test-run/start-and-poll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(infraRunPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value("run_chain"));

        mockMvc.perform(post("/api/v1/ingestion/reportportal/test-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportPortalPayload("run_chain", "PASS", "PASSED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records").value(1));

        mockMvc.perform(post("/api/v1/steps/gates/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gatePayload("run_chain")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PASS"));

        mockMvc.perform(post("/api/v1/steps/gates/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gatePayload("run_chain")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idempotentReplay").value(true));

        mockMvc.perform(get("/api/v1/consultation/runs/run_chain")
                        .param("service", "ms-plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value("run_chain"))
                .andExpect(jsonPath("$.status").value("PASS"))
                .andExpect(jsonPath("$.total").value(1));

        mockMvc.perform(get("/api/v1/consultation/coverage/ff89aa10")
                        .param("service", "ms-plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commitSha").value("ff89aa10"))
                .andExpect(jsonPath("$.service").value("ms-plan"))
                .andExpect(jsonPath("$.testCount").value(1));

        mockMvc.perform(get("/api/v1/tests/com.machine.ms.FooServiceTest.shouldCompute/history")
                        .param("service", "ms-plan")
                        .param("branch", "main")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history").isArray());
    }

    private MockResponse jsonResponse(String body, int status) {
        return new MockResponse()
                .setResponseCode(status)
                .addHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private String toolCall(String toolId, String paramsJson) {
        return """
                {"requestId":"chain-corr-id","toolId":"%s","params":%s}
                """.formatted(toolId, paramsJson);
    }

    private String stepContextParams(String phase) {
        return """
                {"stepId":"step-1959","phase":"%s","baseSha":"ab12cd34","headSha":"ff89aa10","requestId":"chain-step"}
                """.formatted(phase);
    }

    private String infraRunPayload() {
        return """
                {"workflowId":"quality.yml","inputs":{"selectionMode":"IMPACTED_ONLY","tests":["com.machine.ms.FooServiceTest.shouldCompute"]},"pollAttempts":1,"pollSleepMs":200}
                """;
    }

    private String gatePayload(String runId) {
        return """
                {"stepId":"step-1959","phase":"EXECUTION","baseSha":"ab12cd34","headSha":"ff89aa10",
                 "requestId":"chain-gate-req","policyId":"validation-policy-v1","runId":"%s"}
                """.formatted(runId);
    }

    private String commitChangesPayload() {
        return """
                {"requestId":"chain-changes","service":"ms-plan","branch":"main","baseCommit":"ab12cd34","headCommit":"ff89aa10",
                 "changes":[{"filePath":"src/main/java/com/machine/ms/Foo.java","methodSig":"compute()","lineStart":10,"lineEnd":40,"changeType":"MODIFIED"}]}
                """;
    }

    private String openCloverPayload() {
        return """
                {"requestId":"chain-cov","service":"ms-plan","repository":"github.com/org/ms-plan","branch":"main","commitSha":"ff89aa10",
                 "tests":[{"testId":"com.machine.ms.FooServiceTest#shouldCompute","coverage":[{"filePath":"src/main/java/com/machine/ms/Foo.java","className":"Foo","methodSig":"compute()","lineNo":12,"covered":true}]}]}
                """;
    }

    private String reportPortalPayload(String runId, String status, String testStatus) {
        return """
                {"requestId":"chain-rp","service":"ms-plan","branch":"main","commitSha":"ff89aa10","runId":"%s","launchId":"launch_chain","status":"%s",
                 "results":[{"testId":"com.machine.ms.FooServiceTest#shouldCompute","status":"%s","durationMs":120,"errorSignature":null}]}
                """.formatted(runId, status, testStatus);
    }
}
