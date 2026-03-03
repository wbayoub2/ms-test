package com.machine.ms.test.infra.observability;

public final class CorrelationContext {

    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    private CorrelationContext() {
    }

    public static void setRequestId(String requestId) {
        REQUEST_ID.set(requestId);
    }

    public static String getRequestId() {
        return REQUEST_ID.get();
    }

    public static void clear() {
        REQUEST_ID.remove();
    }
}
