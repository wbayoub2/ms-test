package com.machine.ms.test.core.port.in;

import com.machine.ms.test.api.contract.ingestion.ArtifactLinksIngestionRequest;
import com.machine.ms.test.api.contract.ingestion.CommitChangesIngestionRequest;
import com.machine.ms.test.api.contract.ingestion.IngestionResponse;
import com.machine.ms.test.api.contract.ingestion.OpenCloverIngestionRequest;
import com.machine.ms.test.api.contract.ingestion.ReportPortalIngestionRequest;

public interface IngestionUseCase {
    IngestionResponse ingestOpenClover(OpenCloverIngestionRequest request);

    IngestionResponse ingestReportPortal(ReportPortalIngestionRequest request);

    IngestionResponse ingestCommitChanges(CommitChangesIngestionRequest request);

    IngestionResponse ingestArtifacts(ArtifactLinksIngestionRequest request);
}
