package com.machine.ms.test.api.bridge;

import java.util.Map;

public class BridgeInvalidParametersException extends RuntimeException {

    private final Map<String, Object> context;

    public BridgeInvalidParametersException(String message, Map<String, Object> context) {
        super(message);
        this.context = context == null ? Map.of() : Map.copyOf(context);
    }

    public Map<String, Object> context() {
        return context;
    }
}
