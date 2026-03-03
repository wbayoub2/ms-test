package com.machine.ms.test.core.service;

import com.machine.ms.test.core.domain.model.GateReport;

public record GateResult(GateReport report, boolean replay) {
}
