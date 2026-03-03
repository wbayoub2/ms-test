package com.machine.ms.test.core.domain.error;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MsTestException extends RuntimeException {

    private final MsTestErrorCode code;
    private final Map<String, Object> context;

    public MsTestException(MsTestErrorCode code, String message) {
        this(code, message, Map.of());
    }

    public MsTestException(MsTestErrorCode code, String message, Map<String, Object> context) {
        super(message);
        this.code = code;
        this.context = context == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(context));
    }

    public MsTestErrorCode code() {
        return code;
    }

    public Map<String, Object> context() {
        return context;
    }
}
