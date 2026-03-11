package com.machine.ms.test.infra.persistence;

import com.machine.ms.test.core.domain.model.ArtifactLinkSnapshot;
import com.machine.ms.test.core.port.out.ArtifactLinkStore;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryArtifactLinkStore implements ArtifactLinkStore, ResettableInMemoryStore {

    private final Map<String, ArtifactLinkSnapshot> byRunId = new ConcurrentHashMap<>();

    @Override
    public ArtifactLinkSnapshot save(ArtifactLinkSnapshot snapshot) {
        byRunId.put(snapshot.runId(), snapshot);
        return snapshot;
    }

    @Override
    public Optional<ArtifactLinkSnapshot> findByRunId(String runId) {
        return Optional.ofNullable(byRunId.get(runId));
    }

    @Override
    public void reset() {
        byRunId.clear();
    }
}
