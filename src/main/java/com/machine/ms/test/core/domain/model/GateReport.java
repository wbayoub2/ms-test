package com.machine.ms.test.core.domain.model;

import java.time.Instant;
import java.util.List;

public record GateReport(String gateReportId,
                         GateTupleKey tupleKey,
                         String requestId,
                         GateStatus status,
                         List<String> reasons,
                         Instant evaluatedAt) {
}
