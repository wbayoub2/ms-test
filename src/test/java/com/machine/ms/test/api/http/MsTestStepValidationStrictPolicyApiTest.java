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

@SpringBootTest(properties = {
        "mstest.step-validation.diff-policy=STRICT",
        "mstest.step-validation.impact-policy=STRICT"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MsTestStepValidationStrictPolicyApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldBlockSyntheticDiffInStrictMode() throws Exception {
        mockMvc.perform(post("/tools/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toolId":"step_execution_diff_get","params":{"stepId":"step-1959","phase":"EXECUTION","baseSha":"ab12cd34","headSha":"ff89aa10","requestId":"strict-diff"}}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("DIFF_DATA_UNAVAILABLE"))
                .andExpect(jsonPath("$.context.diffPolicy").value("STRICT"));
    }

    @Test
    void shouldBlockDefaultImpactInStrictMode() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/commit-changes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"strict-changes","service":"ms-plan","branch":"main","baseCommit":"ab12cd34","headCommit":"ff89aa10","changes":[{"filePath":"src/main/java/com/machine/ms/ab12cd3/ChangedService.java","methodSig":"compute()","lineStart":12,"lineEnd":12,"changeType":"MODIFIED"}]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/ingestion/openclover/testwise-coverage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"strict-clover","service":"ms-plan","repository":"github.com/org/ms-plan","branch":"main","commitSha":"ff89aa10","tests":[{"testId":"com.machine.ms.OtherServiceTest#shouldStayOut","coverage":[{"filePath":"src/main/java/com/machine/ms/zz99zz9/OtherService.java","className":"OtherService","methodSig":"other()","lineNo":7,"covered":true}]}]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/tools/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toolId":"step_execution_impacted_tests_get","params":{"stepId":"step-1959","phase":"EXECUTION","baseSha":"ab12cd34","headSha":"ff89aa10","requestId":"strict-impact"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impactedTestsCount").value(0))
                .andExpect(jsonPath("$.impactedTests").isArray());
    }
}
