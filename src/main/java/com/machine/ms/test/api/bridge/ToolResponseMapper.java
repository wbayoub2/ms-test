package com.machine.ms.test.api.bridge;

import com.machine.ms.test.api.contract.step.DiffResponse;
import com.machine.ms.test.api.contract.step.GateEvaluateResponse;
import com.machine.ms.test.api.contract.step.ImpactedTestsResponse;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ToolResponseMapper {

    public Map<String, Object> diffResponse(DiffResponse response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("changedFiles", response.filesChanged());
        payload.put("changedFilePaths", response.changedFiles());
        payload.put("diffId", response.diffId());
        payload.put("baseSha", response.baseSha());
        payload.put("headSha", response.headSha());
        payload.put("phase", response.phase().name());
        return payload;
    }

    public Map<String, Object> impactedResponse(ImpactedTestsResponse response, long latencyMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("impactedTestsCount", response.tests().size());
        payload.put("impactedCalcLatencyMs", latencyMs);
        payload.put("impactId", response.impactId());
        payload.put("phase", response.phase().name());
        payload.put("impactedTests", response.tests());
        return payload;
    }

    public Map<String, Object> gateResponse(GateEvaluateResponse response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("gateStatus", response.status().name());
        payload.put("gateReportId", response.gateReportId());
        payload.put("phase", response.phase().name());
        payload.put("idempotentReplay", response.idempotentReplay());
        return payload;
    }
}
