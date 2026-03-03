package com.machine.ms.test.infra.persistence;

import com.machine.ms.test.core.domain.model.TestRunSnapshot;
import com.machine.ms.test.core.port.out.TestRunStore;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTestRunStore implements TestRunStore {

    private final Map<String, TestRunSnapshot> byRunId = new ConcurrentHashMap<>();

    @Override
    public TestRunSnapshot save(TestRunSnapshot snapshot) {
        byRunId.put(snapshot.runId(), snapshot);
        return snapshot;
    }

    @Override
    public Optional<TestRunSnapshot> findByRunId(String runId) {
        return Optional.ofNullable(byRunId.get(runId));
    }

    @Override
    public List<TestRunSnapshot> findByServiceAndBranch(String service, String branch) {
        return byRunId.values().stream()
                .filter(run -> run.service().equals(service) && run.branch().equals(branch))
                .sorted((a, b) -> b.ingestedAt().compareTo(a.ingestedAt()))
                .toList();
    }

    @Override
    public List<TestRunSnapshot> findAll() {
        return byRunId.values().stream()
                .sorted((a, b) -> b.ingestedAt().compareTo(a.ingestedAt()))
                .toList();
    }
}
