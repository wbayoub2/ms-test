package com.machine.ms.test.core.service.functional;

import com.machine.ms.test.core.port.out.ArtifactLinkStore;

public class ArtifactEvidenceRules {

    private final RunSnapshotQueries runQueries;
    private final ArtifactLinkStore artifactLinkStore;

    public ArtifactEvidenceRules(RunSnapshotQueries runQueries, ArtifactLinkStore artifactLinkStore) {
        this.runQueries = runQueries;
        this.artifactLinkStore = artifactLinkStore;
    }

    public boolean hasEvidenceForCommit(String service, String commitSha) {
        return runQueries.allRuns().stream()
                .filter(run -> run.service().equals(service) && run.commitSha().equals(commitSha))
                .anyMatch(run -> artifactLinkStore.findByRunId(run.runId()).isPresent());
    }
}
