package com.machine.ms.test.infra.persistence;

import com.machine.ms.test.core.domain.model.DiffSnapshot;
import com.machine.ms.test.core.domain.model.StepKey;
import com.machine.ms.test.core.port.out.DiffSnapshotStore;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDiffSnapshotStore implements DiffSnapshotStore {

    private final Map<StepKey, DiffSnapshot> snapshots = new ConcurrentHashMap<>();

    @Override
    public Optional<DiffSnapshot> findByStepKey(StepKey key) {
        return Optional.ofNullable(snapshots.get(key));
    }

    @Override
    public DiffSnapshot save(DiffSnapshot snapshot) {
        snapshots.put(snapshot.key(), snapshot);
        return snapshot;
    }
}
