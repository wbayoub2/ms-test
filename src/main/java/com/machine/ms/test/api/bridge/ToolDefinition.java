package com.machine.ms.test.api.bridge;

import java.util.Map;

public record ToolDefinition(String toolName,
                             String description,
                             Map<String, String> metadata) {
}
