package com.machine.ms.test.core.domain.model;

import java.time.Instant;
import java.util.List;

public record CommitChangeSnapshot(String ingestionId,
                                   String requestId,
                                   String service,
                                   String branch,
                                   String baseCommit,
                                   String headCommit,
                                   List<CommitChange> changes,
                                   Instant ingestedAt) {
}
