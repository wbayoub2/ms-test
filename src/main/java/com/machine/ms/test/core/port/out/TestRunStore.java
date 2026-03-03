package com.machine.ms.test.core.port.out;

import com.machine.ms.test.core.domain.model.TestRunSnapshot;

import java.util.List;
import java.util.Optional;

public interface TestRunStore {
    TestRunSnapshot save(TestRunSnapshot snapshot);

    Optional<TestRunSnapshot> findByRunId(String runId);

    List<TestRunSnapshot> findByServiceAndBranch(String service, String branch);

    List<TestRunSnapshot> findAll();
}
