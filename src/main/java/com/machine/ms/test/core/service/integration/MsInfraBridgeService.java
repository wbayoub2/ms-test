package com.machine.ms.test.core.service.integration;

import com.machine.ms.test.api.contract.integration.MsInfraRunRequest;
import com.machine.ms.test.core.domain.error.MsTestErrorCode;
import com.machine.ms.test.core.domain.error.MsTestException;
import com.machine.ms.test.core.port.out.MsInfraToolPort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MsInfraBridgeService {

    private final MsInfraToolPort msInfraToolPort;

    public MsInfraBridgeService(MsInfraToolPort msInfraToolPort) {
        this.msInfraToolPort = msInfraToolPort;
    }

    public Map<String, Object> startAndPoll(MsInfraRunRequest request) {
        return startAndPoll(request, null);
    }

    public Map<String, Object> startAndPoll(MsInfraRunRequest request, String correlationId) {
        Map<String, Object> startResult = msInfraToolPort.startTestRun(startPayload(request), correlationId);
        if (startResult.containsKey("errorCode")) {
            throw new MsTestException(MsTestErrorCode.UPSTREAM_UNAVAILABLE,
                    "ms-infra test_run_start failed",
                    Map.of(
                            "errorCode", String.valueOf(startResult.get("errorCode")),
                            "message", String.valueOf(startResult.get("message"))));
        }
        String runId = String.valueOf(startResult.get("runId"));
        if (runId.isBlank() || "null".equalsIgnoreCase(runId)) {
            throw new MsTestException(MsTestErrorCode.UPSTREAM_UNAVAILABLE, "ms-infra returned empty runId");
        }
        Map<String, Object> last = startResult;
        int attempts = request.pollAttempts() <= 0 ? 5 : request.pollAttempts();
        long sleepMs = request.pollSleepMs() <= 0 ? 1500 : request.pollSleepMs();
        for (int i = 0; i < attempts; i++) {
            last = msInfraToolPort.getTestRun(runId, correlationId);
            String status = String.valueOf(last.get("status"));
            if (!"RUNNING".equalsIgnoreCase(status) && !"IN_PROGRESS".equalsIgnoreCase(status)) {
                break;
            }
            sleep(sleepMs);
        }
        return canonicalRunResult(runId, last);
    }

    private Map<String, Object> startPayload(MsInfraRunRequest request) {
        Map<String, Object> inputs = request.inputs() == null ? Map.of() : request.inputs();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("runId", stringValue(request.runId(), inputs.get("runId")));
        payload.put("target", stringValue(request.target(), inputs.get("target"), defaultTarget(request.workflowId())));
        payload.put("mode", stringValue(request.mode(), inputs.get("mode"), "mock"));
        payload.put("runtimeMode", stringValue(request.runtimeMode(), inputs.get("runtimeMode"), "auto"));
        payload.put("strict", booleanValue(request.strict(), inputs.get("strict"), true));
        payload.put("keepArtifacts", booleanValue(request.keepArtifacts(), inputs.get("keepArtifacts"), true));
        return payload;
    }

    private String defaultTarget(String workflowId) {
        if (workflowId == null) {
            return "full-chain";
        }
        String normalized = workflowId.toLowerCase();
        if (normalized.contains("doc")) {
            return "doc-chain";
        }
        if (normalized.contains("mcp")) {
            return "mcp-chain";
        }
        if (normalized.contains("bridge")) {
            return "bridge";
        }
        return "full-chain";
    }

    private String stringValue(Object... values) {
        for (Object value : values) {
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private boolean booleanValue(Boolean first, Object second, boolean fallback) {
        if (first != null) {
            return first;
        }
        if (second instanceof Boolean bool) {
            return bool;
        }
        if (second instanceof String text && !text.isBlank()) {
            return Boolean.parseBoolean(text);
        }
        return fallback;
    }

    private void sleep(long sleepMs) {
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private Map<String, Object> canonicalRunResult(String runId, Map<String, Object> last) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("runId", runId);
        if (last != null) {
            payload.putAll(last);
        }
        payload.put("runId", runId);
        return payload;
    }
}
