package com.machine.ms.test.infra.persistence;

import com.machine.ms.test.core.domain.model.GateReport;
import com.machine.ms.test.core.domain.model.GateTupleKey;
import com.machine.ms.test.core.port.out.GateReportStore;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryGateReportStore implements GateReportStore {

    private final Map<GateTupleKey, GateReport> byTuple = new ConcurrentHashMap<>();
    private final Map<String, GateTupleKey> tupleByRequestId = new ConcurrentHashMap<>();

    @Override
    public Optional<GateReport> findByTupleKey(GateTupleKey key) {
        return Optional.ofNullable(byTuple.get(key));
    }

    @Override
    public Optional<GateTupleKey> findTupleByRequestId(String requestId) {
        return Optional.ofNullable(tupleByRequestId.get(requestId));
    }

    @Override
    public Optional<GateReport> findLatestByRunId(String runId) {
        return byTuple.values().stream()
                .filter(report -> report.tupleKey().runId().equals(runId))
                .max(java.util.Comparator.comparing(GateReport::evaluatedAt));
    }

    @Override
    public GateReport save(GateReport report) {
        byTuple.put(report.tupleKey(), report);
        tupleByRequestId.put(report.requestId(), report.tupleKey());
        return report;
    }
}
