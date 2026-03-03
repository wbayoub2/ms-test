package com.machine.ms.test.core.port.out;

import com.machine.ms.test.core.domain.model.ImpactSnapshot;
import com.machine.ms.test.core.domain.model.StepKey;

import java.util.Optional;

public interface ImpactSnapshotStore {
    Optional<ImpactSnapshot> findByStepKey(StepKey key);

    ImpactSnapshot save(ImpactSnapshot snapshot);
}
