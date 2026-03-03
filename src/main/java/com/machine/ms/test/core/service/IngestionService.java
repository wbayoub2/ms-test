package com.machine.ms.test.core.service;

import com.machine.ms.test.api.contract.ingestion.ArtifactLinksIngestionRequest;
import com.machine.ms.test.api.contract.ingestion.CommitChangesIngestionRequest;
import com.machine.ms.test.api.contract.ingestion.IngestionResponse;
import com.machine.ms.test.api.contract.ingestion.OpenCloverIngestionRequest;
import com.machine.ms.test.api.contract.ingestion.ReportPortalIngestionRequest;
import com.machine.ms.test.core.domain.model.ArtifactLink;
import com.machine.ms.test.core.domain.model.ArtifactLinkSnapshot;
import com.machine.ms.test.core.domain.model.CommitChange;
import com.machine.ms.test.core.domain.model.CommitChangeSnapshot;
import com.machine.ms.test.core.domain.model.CoverageLine;
import com.machine.ms.test.core.domain.model.CoverageSnapshot;
import com.machine.ms.test.core.domain.model.TestCoverageRecord;
import com.machine.ms.test.core.domain.model.TestResult;
import com.machine.ms.test.core.domain.model.TestRunSnapshot;
import com.machine.ms.test.core.port.in.IngestionUseCase;
import com.machine.ms.test.core.port.out.ArtifactLinkStore;
import com.machine.ms.test.core.port.out.CommitChangeStore;
import com.machine.ms.test.core.port.out.CoverageStore;
import com.machine.ms.test.core.port.out.TestRunStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class IngestionService implements IngestionUseCase {

    private final CoverageStore coverageStore;
    private final TestRunStore testRunStore;
    private final CommitChangeStore commitChangeStore;
    private final ArtifactLinkStore artifactLinkStore;
    private final AtomicLong ingestionSequence = new AtomicLong(1);

    public IngestionService(CoverageStore coverageStore,
                            TestRunStore testRunStore,
                            CommitChangeStore commitChangeStore,
                            ArtifactLinkStore artifactLinkStore) {
        this.coverageStore = coverageStore;
        this.testRunStore = testRunStore;
        this.commitChangeStore = commitChangeStore;
        this.artifactLinkStore = artifactLinkStore;
    }

    @Override
    public IngestionResponse ingestOpenClover(OpenCloverIngestionRequest request) {
        Instant now = Instant.now();
        String ingestionId = "clover_" + ingestionSequence.getAndIncrement();
        List<TestCoverageRecord> tests = request.tests().stream().map(test -> new TestCoverageRecord(
                test.testId(),
                test.coverage().stream().map(line -> new CoverageLine(
                        line.filePath(),
                        line.className(),
                        line.methodSig(),
                        line.lineNo(),
                        line.covered())).toList())).toList();
        coverageStore.save(new CoverageSnapshot(ingestionId, request.requestId(), request.service(), request.repository(),
                request.branch(), request.commitSha(), tests, now));
        return new IngestionResponse(request.requestId(), ingestionId, "OPEN_CLOVER", tests.size(), now);
    }

    @Override
    public IngestionResponse ingestReportPortal(ReportPortalIngestionRequest request) {
        Instant now = Instant.now();
        String ingestionId = "rp_" + ingestionSequence.getAndIncrement();
        List<TestResult> results = request.results().stream()
                .map(result -> new TestResult(result.testId(), result.status(), result.durationMs(), result.errorSignature()))
                .toList();
        testRunStore.save(new TestRunSnapshot(ingestionId, request.requestId(), request.service(), request.branch(),
                request.commitSha(), request.runId(), request.launchId(), request.status(), results, now));
        return new IngestionResponse(request.requestId(), ingestionId, "REPORT_PORTAL", results.size(), now);
    }

    @Override
    public IngestionResponse ingestCommitChanges(CommitChangesIngestionRequest request) {
        Instant now = Instant.now();
        String ingestionId = "chg_" + ingestionSequence.getAndIncrement();
        List<CommitChange> changes = request.changes().stream()
                .map(change -> new CommitChange(change.filePath(), change.methodSig(), change.lineStart(), change.lineEnd(), change.changeType()))
                .toList();
        commitChangeStore.save(new CommitChangeSnapshot(ingestionId, request.requestId(), request.service(), request.branch(),
                request.baseCommit(), request.headCommit(), changes, now));
        return new IngestionResponse(request.requestId(), ingestionId, "COMMIT_CHANGES", changes.size(), now);
    }

    @Override
    public IngestionResponse ingestArtifacts(ArtifactLinksIngestionRequest request) {
        Instant now = Instant.now();
        String ingestionId = "art_" + ingestionSequence.getAndIncrement();
        List<ArtifactLink> artifacts = request.artifacts().stream()
                .map(artifact -> new ArtifactLink(artifact.artifactType(), artifact.storage(), artifact.url(), artifact.checksum()))
                .toList();
        artifactLinkStore.save(new ArtifactLinkSnapshot(ingestionId, request.requestId(), request.runId(), artifacts, now));
        return new IngestionResponse(request.requestId(), ingestionId, "ARTIFACT_LINKS", artifacts.size(), now);
    }
}
