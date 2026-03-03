package com.machine.ms.test.api.contract.consultation;

import java.time.Instant;

public record CoverageConsultationResponse(String requestId,
                                           String ingestionId,
                                           String service,
                                           String repository,
                                           String branch,
                                           String commitSha,
                                           int testCount,
                                           Instant ingestedAt) {
}
