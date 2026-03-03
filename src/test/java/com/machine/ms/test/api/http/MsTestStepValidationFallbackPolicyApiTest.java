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
        "mstest.step-validation.diff-policy=ALLOW_SYNTHETIC_FALLBACK",
        "mstest.step-validation.impact-policy=ALLOW_DEFAULT_FALLBACK"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MsTestStepValidationFallbackPolicyApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeSyntheticDiffAndDefaultImpactInFallbackMode() throws Exception {
        mockMvc.perform(post("/tools/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toolId":"step_execution_diff_get","params":{"stepId":"step-1959","phase":"EXECUTION","baseSha":"ab12cd34","headSha":"ff89aa10","requestId":"fallback-diff"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changedFiles").value(2))
                .andExpect(jsonPath("$.changedFilePaths[0]").value("src/main/java/com/machine/ms/ab12cd3/ChangedService.java"));

        mockMvc.perform(post("/tools/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toolId":"step_execution_impacted_tests_get","params":{"stepId":"step-1959","phase":"EXECUTION","baseSha":"ab12cd34","headSha":"ff89aa10","requestId":"fallback-impact"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impactedTestsCount").value(1))
                .andExpect(jsonPath("$.impactedTests[0].testId").value("com.machine.ms.test.DefaultImpactedTest#shouldRun"));
    }
}
