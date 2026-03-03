package com.machine.ms.test.api.bridge;

import com.machine.ms.test.api.contract.step.GateEvaluateRequest;
import com.machine.ms.test.api.contract.step.StepContextRequest;
import com.machine.ms.test.core.domain.model.StepPhase;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

@Component
public class ToolParamsMapper {

    private static final Pattern STRICT_SHA_PATTERN = Pattern.compile("^[a-f0-9]{7,64}$");
    private static final Pattern BRIDGE_SHA_SAMPLE_PATTERN = Pattern.compile("^[a-zA-Z0-9]{3,64}$");

    public StepContextRequest toStepContext(String toolId, String topLevelRequestId, Map<String, Object> params) {
        String requestId = resolveRequestId(topLevelRequestId, params);
        String stepId = requiredString(params, "stepId");
        StepPhase phase = resolvePhase(toolId, params);
        String baseSha = requiredSha(params, "baseSha");
        String headSha = requiredSha(params, "headSha");
        return new StepContextRequest(stepId, phase, baseSha, headSha, requestId);
    }

    public GateEvaluateRequest toGateRequest(String toolId, String topLevelRequestId, Map<String, Object> params) {
        StepContextRequest context = toStepContext(toolId, topLevelRequestId, params);
        String policyId = requiredString(params, "policyId");
        String runId = requiredString(params, "runId");
        return new GateEvaluateRequest(
                context.stepId(),
                context.phase(),
                context.baseSha(),
                context.headSha(),
                context.requestId(),
                policyId,
                runId);
    }

    private String resolveRequestId(String topLevelRequestId, Map<String, Object> params) {
        String fromParams = optionalString(params, "requestId");
        if (fromParams != null) {
            return fromParams;
        }
        if (topLevelRequestId != null && !topLevelRequestId.isBlank()) {
            return topLevelRequestId;
        }
        return "mstest-tool-" + UUID.randomUUID();
    }

    private StepPhase resolvePhase(String toolId, Map<String, Object> params) {
        String raw = optionalString(params, "phase");
        if (raw != null) {
            try {
                return StepPhase.valueOf(raw.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                throw invalid("Unsupported phase", Map.of("param", "phase", "value", raw));
            }
        }
        if (ToolRegistry.STEP_EXECUTION_DIFF_GET.equals(toolId)
                || ToolRegistry.STEP_EXECUTION_IMPACTED_TESTS_GET.equals(toolId)
                || ToolRegistry.STEP_EXECUTION_GATE_EVALUATE.equals(toolId)) {
            return StepPhase.EXECUTION;
        }
        return StepPhase.REVIEW;
    }

    private String requiredSha(Map<String, Object> params, String key) {
        String value = requiredString(params, key);
        if (STRICT_SHA_PATTERN.matcher(value).matches()) {
            return value;
        }
        if (BRIDGE_SHA_SAMPLE_PATTERN.matcher(value).matches()) {
            return sha1Hex(value);
        }
        if (!STRICT_SHA_PATTERN.matcher(value).matches() && !BRIDGE_SHA_SAMPLE_PATTERN.matcher(value).matches()) {
            throw invalid("Invalid " + key + " format", Map.of("param", key, "value", value));
        }
        return value;
    }

    private String sha1Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 digest unavailable", e);
        }
    }

    private String requiredString(Map<String, Object> params, String key) {
        String value = optionalString(params, key);
        if (value == null) {
            throw invalid("Missing " + key, Map.of("param", key));
        }
        return value;
    }

    private String optionalString(Map<String, Object> params, String key) {
        if (params == null || !params.containsKey(key)) {
            return null;
        }
        Object value = params.get(key);
        if (value == null) {
            return null;
        }
        String asString = value.toString().trim();
        return asString.isBlank() ? null : asString;
    }

    private BridgeInvalidParametersException invalid(String message, Map<String, Object> context) {
        return new BridgeInvalidParametersException(message, new LinkedHashMap<>(context));
    }
}
