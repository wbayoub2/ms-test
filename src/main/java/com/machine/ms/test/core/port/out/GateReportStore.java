package com.machine.ms.test.core.port.out;

import com.machine.ms.test.core.domain.model.GateReport;
import com.machine.ms.test.core.domain.model.GateTupleKey;

import java.util.Optional;

public interface GateReportStore {
    Optional<GateReport> findByTupleKey(GateTupleKey key);

    Optional<GateTupleKey> findTupleByRequestId(String requestId);

    Optional<GateReport> findLatestByRunId(String runId);

    GateReport save(GateReport report);
}
