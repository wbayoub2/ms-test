package com.machine.ms.test.infra.persistence;

import com.machine.ms.test.core.domain.model.CommitChangeSnapshot;
import com.machine.ms.test.core.port.out.CommitChangeStore;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCommitChangeStore implements CommitChangeStore, ResettableInMemoryStore {

    private final Map<String, CommitChangeSnapshot> snapshots = new ConcurrentHashMap<>();

    @Override
    public CommitChangeSnapshot save(CommitChangeSnapshot snapshot) {
        snapshots.put(key(snapshot.service(), snapshot.headCommit()), snapshot);
        return snapshot;
    }

    @Override
    public Optional<CommitChangeSnapshot> findByHeadCommit(String service, String headCommit) {
        return Optional.ofNullable(snapshots.get(key(service, headCommit)));
    }

    @Override
    public void reset() {
        snapshots.clear();
    }

    private String key(String service, String headCommit) {
        return service + "::" + headCommit;
    }
}
