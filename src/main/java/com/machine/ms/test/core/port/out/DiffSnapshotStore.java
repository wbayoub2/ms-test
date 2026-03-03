package com.machine.ms.test.core.port.out;

import com.machine.ms.test.core.domain.model.DiffSnapshot;
import com.machine.ms.test.core.domain.model.StepKey;

import java.util.Optional;

public interface DiffSnapshotStore {
    Optional<DiffSnapshot> findByStepKey(StepKey key);

    DiffSnapshot save(DiffSnapshot snapshot);
}
