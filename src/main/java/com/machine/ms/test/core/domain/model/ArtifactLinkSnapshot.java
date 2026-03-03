package com.machine.ms.test.core.domain.model;

import java.time.Instant;
import java.util.List;

public record ArtifactLinkSnapshot(String ingestionId,
                                   String requestId,
                                   String runId,
                                   java.util.List<ArtifactLink> artifacts,
                                   Instant ingestedAt) {
}
