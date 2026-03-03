package com.machine.ms.test.core.service.functional;

import com.machine.ms.test.core.domain.model.TestResult;
import com.machine.ms.test.core.domain.model.TestRunSnapshot;
import com.machine.ms.test.core.port.out.TestRunStore;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class RunSnapshotQueries {

    private final TestRunStore testRunStore;

    public RunSnapshotQueries(TestRunStore testRunStore) {
        this.testRunStore = testRunStore;
    }

    public List<TestRunSnapshot> runs(String service, String branch) {
        return testRunStore.findByServiceAndBranch(service, branch);
    }

    public Optional<TestRunSnapshot> runByCommit(String service, String branch, String commitSha) {
        return runs(service, branch).stream()
                .filter(run -> run.commitSha().equals(commitSha))
                .max(Comparator.comparing(TestRunSnapshot::ingestedAt));
    }

    public Optional<TestResult> testResultAtCommit(String service, String branch, String commitSha, String testId) {
        return runByCommit(service, branch, commitSha)
                .flatMap(run -> run.results().stream().filter(result -> result.testId().equals(testId)).findFirst());
    }

    public List<TestRunSnapshot> allRuns() {
        return testRunStore.findAll();
    }
}
