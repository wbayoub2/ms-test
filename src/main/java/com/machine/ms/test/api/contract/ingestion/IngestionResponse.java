package com.machine.ms.test.api.contract.ingestion;

import java.time.Instant;

public record IngestionResponse(String requestId,
                                String ingestionId,
                                String source,
                                long records,
                                Instant ingestedAt) {
}
