package com.machine.ms.test.api.contract.integration;

import java.util.List;
import java.util.Map;

public record MsInfraE2eConsultationResponse(String runId,
                                             String status,
                                             String gateStatus,
                                             String reportPortalLaunchId,
                                             String reportPortalUrl,
                                             List<String> impactedTests,
                                             List<Map<String, Object>> evidence,
                                             Map<String, Object> upstream) {
}
