package com.machine.ms.test.api.bridge;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ToolRegistry {

    public static final String STEP_EXECUTION_DIFF_GET = "step_execution_diff_get";
    public static final String STEP_REVIEW_DIFF_GET = "step_review_diff_get";
    public static final String STEP_EXECUTION_IMPACTED_TESTS_GET = "step_execution_impacted_tests_get";
    public static final String STEP_REVIEW_IMPACTED_TESTS_GET = "step_review_impacted_tests_get";
    public static final String STEP_GATE_EVALUATE = "step_gate_evaluate";
    public static final String STEP_REVIEW_GATE_EVALUATE = "step_review_gate_evaluate";
    public static final String STEP_EXECUTION_GATE_EVALUATE = "step_execution_gate_evaluate";

    private static final Map<String, String> METADATA = Map.of("source", "ms-test", "version", "v1");

    private static final List<ToolDefinition> TOOLS = List.of(
            new ToolDefinition(STEP_EXECUTION_DIFF_GET, "Read execution diff metadata", METADATA),
            new ToolDefinition(STEP_REVIEW_DIFF_GET, "Read review diff metadata", METADATA),
            new ToolDefinition(STEP_EXECUTION_IMPACTED_TESTS_GET, "Read execution impacted tests", METADATA),
            new ToolDefinition(STEP_REVIEW_IMPACTED_TESTS_GET, "Read review impacted tests", METADATA),
            new ToolDefinition(STEP_GATE_EVALUATE, "Evaluate gate result", METADATA),
            new ToolDefinition(STEP_REVIEW_GATE_EVALUATE, "Evaluate review gate result", METADATA),
            new ToolDefinition(STEP_EXECUTION_GATE_EVALUATE, "Evaluate execution gate result", METADATA)
    );

    private static final Set<String> TOOL_NAMES = Set.of(
            STEP_EXECUTION_DIFF_GET,
            STEP_REVIEW_DIFF_GET,
            STEP_EXECUTION_IMPACTED_TESTS_GET,
            STEP_REVIEW_IMPACTED_TESTS_GET,
            STEP_GATE_EVALUATE,
            STEP_REVIEW_GATE_EVALUATE,
            STEP_EXECUTION_GATE_EVALUATE
    );

    public List<ToolDefinition> catalog() {
        return TOOLS;
    }

    public boolean supports(String toolId) {
        return toolId != null && TOOL_NAMES.contains(toolId);
    }
}
