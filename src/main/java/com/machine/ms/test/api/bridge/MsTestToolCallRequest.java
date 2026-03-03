package com.machine.ms.test.api.bridge;

import java.util.Map;

public record MsTestToolCallRequest(String requestId,
                                    String toolId,
                                    Map<String, Object> params) {
}
