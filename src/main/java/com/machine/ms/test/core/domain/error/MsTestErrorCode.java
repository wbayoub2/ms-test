package com.machine.ms.test.core.domain.error;

public enum MsTestErrorCode {
    INVALID_INPUT(MsTestErrorType.VALIDATION),
    TEST_NOT_FOUND(MsTestErrorType.DOMAIN),
    COMMIT_NOT_FOUND(MsTestErrorType.DOMAIN),
    EVIDENCE_NOT_FOUND(MsTestErrorType.DOMAIN),
    IMPACT_COMPUTE_FAILED(MsTestErrorType.DOMAIN),
    DIFF_DATA_UNAVAILABLE(MsTestErrorType.DOMAIN),
    IMPACT_DATA_UNAVAILABLE(MsTestErrorType.DOMAIN),
    GATE_IDEMPOTENCY_CONFLICT(MsTestErrorType.DOMAIN),
    UNSUPPORTED_STEP_PHASE(MsTestErrorType.VALIDATION),
    UPSTREAM_UNAVAILABLE(MsTestErrorType.PROVIDER),
    SNAPSHOT_NOT_FOUND(MsTestErrorType.DOMAIN),
    RUN_NOT_FOUND(MsTestErrorType.DOMAIN);

    private final MsTestErrorType type;

    MsTestErrorCode(MsTestErrorType type) {
        this.type = type;
    }

    public MsTestErrorType type() {
        return type;
    }
}
