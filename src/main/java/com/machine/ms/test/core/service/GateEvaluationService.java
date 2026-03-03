package com.machine.ms.test.core.service;

import com.machine.ms.test.core.domain.error.MsTestErrorCode;
import com.machine.ms.test.core.domain.error.MsTestException;
import com.machine.ms.test.core.domain.model.GateReport;
import com.machine.ms.test.core.domain.model.GateStatus;
import com.machine.ms.test.core.domain.model.GateTupleKey;
import com.machine.ms.test.core.port.out.GateReportStore;
import com.machine.ms.test.core.port.out.TestRunStore;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class GateEvaluationService {

    private final GateReportStore gateReportStore;
    private final TestRunStore testRunStore;
    private final StepIdGenerator idGenerator;

    public GateEvaluationService(GateReportStore gateReportStore, TestRunStore testRunStore, StepIdGenerator idGenerator) {
        this.gateReportStore = gateReportStore;
        this.testRunStore = testRunStore;
        this.idGenerator = idGenerator;
    }

    public GateResult evaluate(String requestId, GateTupleKey tupleKey) {
        gateReportStore.findTupleByRequestId(requestId)
                .filter(existing -> !existing.equals(tupleKey))
                .ifPresent(existing -> {
                    throw new MsTestException(
                            MsTestErrorCode.GATE_IDEMPOTENCY_CONFLICT,
                            "requestId already bound to a different gate tuple",
                            Map.of("requestId", requestId));
                });

        GateReport replay = gateReportStore.findByTupleKey(tupleKey).orElse(null);
        if (replay != null) {
            return new GateResult(replay, true);
        }

        GateStatus status = GateStatus.PASS;
        List<String> reasons = List.of();
        if (testRunStore.findByRunId(tupleKey.runId()).isEmpty()) {
            throw new MsTestException(MsTestErrorCode.RUN_NOT_FOUND, "runId not found", Map.of("runId", tupleKey.runId()));
        }
        long failed = testRunStore.findByRunId(tupleKey.runId()).orElseThrow().failedCount();
        if (failed > 0) {
            status = GateStatus.FAIL;
            reasons = List.of("failed_tests_present");
        }

        GateReport report = new GateReport(idGenerator.nextGateId(), tupleKey, requestId, status, reasons, Instant.now());
        gateReportStore.save(report);
        return new GateResult(report, false);
    }
}
