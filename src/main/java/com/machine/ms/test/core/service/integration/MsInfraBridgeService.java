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
        Map<String, Object> startResult = msInfraToolPort.startTestRun(
                request.workflowId(),
                request.inputs() == null ? Map.of() : request.inputs(),
                correlationId);
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
