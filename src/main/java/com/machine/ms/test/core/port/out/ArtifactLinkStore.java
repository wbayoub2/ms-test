package com.machine.ms.test.core.port.out;

import com.machine.ms.test.core.domain.model.ArtifactLinkSnapshot;

import java.util.Optional;

public interface ArtifactLinkStore {

    ArtifactLinkSnapshot save(ArtifactLinkSnapshot snapshot);

    Optional<ArtifactLinkSnapshot> findByRunId(String runId);
}
