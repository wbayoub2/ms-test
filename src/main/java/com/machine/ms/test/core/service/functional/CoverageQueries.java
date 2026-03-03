package com.machine.ms.test.core.service.functional;

import com.machine.ms.test.core.domain.model.CoverageSnapshot;
import com.machine.ms.test.core.domain.model.TestCoverageRecord;
import com.machine.ms.test.core.port.out.CoverageStore;

import java.util.List;
import java.util.Optional;

public class CoverageQueries {

    private final CoverageStore coverageStore;

    public CoverageQueries(CoverageStore coverageStore) {
        this.coverageStore = coverageStore;
    }

    public Optional<CoverageSnapshot> byCommit(String commitSha) {
        return coverageStore.findByCommitSha(commitSha);
    }

    public Optional<TestCoverageRecord> testAtCommit(String commitSha, String testId) {
        return byCommit(commitSha).flatMap(snapshot ->
                snapshot.tests().stream().filter(test -> test.testId().equals(testId)).findFirst());
    }

    public List<CoverageSnapshot> byServiceAndBranch(String service, String branch) {
        return coverageStore.findByServiceAndBranch(service, branch);
    }

    public List<CoverageSnapshot> all() {
        return coverageStore.findAll();
    }
}
