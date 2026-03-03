package com.machine.ms.test.api.http;

import com.machine.ms.test.api.contract.ingestion.ArtifactLinksIngestionRequest;
import com.machine.ms.test.api.contract.ingestion.CommitChangesIngestionRequest;
import com.machine.ms.test.api.contract.ingestion.IngestionResponse;
import com.machine.ms.test.api.contract.ingestion.OpenCloverIngestionRequest;
import com.machine.ms.test.api.contract.ingestion.ReportPortalIngestionRequest;
import com.machine.ms.test.core.port.in.IngestionUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ingestion")
public class IngestionController {

    private final IngestionUseCase ingestionUseCase;

    public IngestionController(IngestionUseCase ingestionUseCase) {
        this.ingestionUseCase = ingestionUseCase;
    }

    @PostMapping("/openclover/testwise-coverage")
    public IngestionResponse ingestOpenClover(@Valid @RequestBody OpenCloverIngestionRequest request) {
        return ingestionUseCase.ingestOpenClover(request);
    }

    @PostMapping("/reportportal/test-runs")
    public IngestionResponse ingestReportPortal(@Valid @RequestBody ReportPortalIngestionRequest request) {
        return ingestionUseCase.ingestReportPortal(request);
    }

    @PostMapping("/commit-changes")
    public IngestionResponse ingestCommitChanges(@Valid @RequestBody CommitChangesIngestionRequest request) {
        return ingestionUseCase.ingestCommitChanges(request);
    }

    @PostMapping("/artifacts")
    public IngestionResponse ingestArtifacts(@Valid @RequestBody ArtifactLinksIngestionRequest request) {
        return ingestionUseCase.ingestArtifacts(request);
    }
}
