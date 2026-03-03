package com.machine.ms.test.infra.persistence;

import com.machine.ms.test.core.domain.model.ImpactSnapshot;
import com.machine.ms.test.core.domain.model.StepKey;
import com.machine.ms.test.core.port.out.ImpactSnapshotStore;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryImpactSnapshotStore implements ImpactSnapshotStore {

    private final Map<StepKey, ImpactSnapshot> snapshots = new ConcurrentHashMap<>();

    @Override
    public Optional<ImpactSnapshot> findByStepKey(StepKey key) {
        return Optional.ofNullable(snapshots.get(key));
    }

    @Override
    public ImpactSnapshot save(ImpactSnapshot snapshot) {
        snapshots.put(snapshot.key(), snapshot);
        return snapshot;
    }
}
