package com.machine.ms.test.core.port.out;

import java.util.Map;

public interface MsInfraToolPort {

    Map<String, Object> startTestRun(String workflowId, Map<String, Object> inputs, String correlationId);

    Map<String, Object> getTestRun(String runId, String correlationId);
}
