package com.machine.ms.test.core.port.out;

import java.util.Map;

public interface MsInfraToolPort {

    Map<String, Object> startTestRun(Map<String, Object> payload, String correlationId);

    Map<String, Object> getTestRun(String runId, String correlationId);

    Map<String, Object> getTestRunArtifacts(String runId, String correlationId);
}
