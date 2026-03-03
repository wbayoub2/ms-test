package com.machine.ms.test.api.contract.consultation;

import java.time.Instant;

public record RunConsultationResponse(String requestId,
                                      String ingestionId,
                                      String service,
                                      String branch,
                                      String commitSha,
                                      String runId,
                                      String launchId,
                                      String status,
                                      int total,
                                      int failed,
                                      Instant ingestedAt) {
}
