package com.machine.ms.test.core.service;

import java.util.concurrent.atomic.AtomicLong;

public class StepIdGenerator {

    private final AtomicLong diffSequence = new AtomicLong(1);
    private final AtomicLong impactSequence = new AtomicLong(1);
    private final AtomicLong gateSequence = new AtomicLong(1);

    public String nextDiffId() {
        return "diff_" + diffSequence.getAndIncrement();
    }

    public String nextImpactId() {
        return "impact_" + impactSequence.getAndIncrement();
    }

    public String nextGateId() {
        return "gate_" + gateSequence.getAndIncrement();
    }
}
