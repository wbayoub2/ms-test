package com.machine.ms.test.api.http;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MsTestCommonStarterApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesActuatorHealthAndCorrelationHeaders() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .header("X-Request-Id", "starter-req-1")
                        .header("X-Session-Id", "starter-session-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "starter-req-1"))
                .andExpect(header().string("X-Session-Id", "starter-session-1"))
                .andExpect(header().exists("X-Instance-Id"))
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
