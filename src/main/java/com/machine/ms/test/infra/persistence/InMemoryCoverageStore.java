package com.machine.ms.test.infra.persistence;

import com.machine.ms.test.core.domain.model.CoverageSnapshot;
import com.machine.ms.test.core.port.out.CoverageStore;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCoverageStore implements CoverageStore {

    private final Map<String, CoverageSnapshot> byCommit = new ConcurrentHashMap<>();

    @Override
    public CoverageSnapshot save(CoverageSnapshot snapshot) {
        byCommit.put(snapshot.commitSha(), snapshot);
        return snapshot;
    }

    @Override
    public Optional<CoverageSnapshot> findByCommitSha(String commitSha) {
        return Optional.ofNullable(byCommit.get(commitSha));
    }

    @Override
    public List<CoverageSnapshot> findByServiceAndBranch(String service, String branch) {
        return byCommit.values().stream()
                .filter(snapshot -> snapshot.service().equals(service) && snapshot.branch().equals(branch))
                .sorted((a, b) -> b.ingestedAt().compareTo(a.ingestedAt()))
                .toList();
    }

    @Override
    public List<CoverageSnapshot> findAll() {
        return byCommit.values().stream()
                .sorted((a, b) -> b.ingestedAt().compareTo(a.ingestedAt()))
                .toList();
    }
}
