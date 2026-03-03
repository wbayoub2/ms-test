package com.machine.ms.test.infra.integration;

import com.machine.ms.test.core.domain.error.MsTestErrorCode;
import com.machine.ms.test.core.domain.error.MsTestException;
import com.machine.ms.test.core.port.out.MsInfraToolPort;
import com.machine.ms.test.infra.config.MsInfraClientProperties;
import com.machine.ms.test.infra.observability.CorrelationContext;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MsInfraHttpToolClient implements MsInfraToolPort {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String SESSION_ID_HEADER = "X-Session-Id";
    private final MsInfraClientProperties properties;
    private final RestTemplate restTemplate;

    public MsInfraHttpToolClient(MsInfraClientProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .build();
    }

    @Override
    public Map<String, Object> startTestRun(String workflowId, Map<String, Object> inputs, String correlationId) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("workflowId", workflowId);
        params.put("inputs", inputs == null ? Map.of() : inputs);
        return callWithRetry("infra.test_run_start", params, correlationId);
    }

    @Override
    public Map<String, Object> getTestRun(String runId, String correlationId) {
        return callWithRetry("infra.test_run_get", Map.of("runId", runId), correlationId);
    }

    private Map<String, Object> callWithRetry(String toolId, Map<String, Object> params, String correlationId) {
        if (!properties.isEnabled()) {
            throw new MsTestException(MsTestErrorCode.UPSTREAM_UNAVAILABLE, "ms-infra integration disabled");
        }
        int attempts = Math.max(1, properties.getMaxAttempts());
        RuntimeException lastFailure = null;
        String requestId = resolveRequestId(correlationId);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return postToolsCall(toolPayload(toolId, params, requestId), headers(requestId));
            } catch (RestClientException restClientException) {
                lastFailure = restClientException;
                sleep(properties.getRetryDelayMs());
            }
        }
        throw new MsTestException(MsTestErrorCode.UPSTREAM_UNAVAILABLE,
                "ms-infra call failed after retries",
                Map.of("toolId", toolId, "reason", lastFailure == null ? "unknown" : lastFailure.getMessage()));
    }

    private Map<String, Object> postToolsCall(Map<String, Object> payload, HttpHeaders headers) {
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        Map<?, ?> raw = restTemplate.postForObject(properties.getBaseUrl() + "/tools/call", request, Map.class);
        if (raw == null) {
            throw new MsTestException(MsTestErrorCode.UPSTREAM_UNAVAILABLE, "ms-infra returned empty response");
        }
        return castMap(raw);
    }

    private HttpHeaders headers(String requestId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(REQUEST_ID_HEADER, requestId);
        if (properties.getCookie() != null && !properties.getCookie().isBlank()) {
            headers.add(HttpHeaders.COOKIE, properties.getCookie());
        }
        if (properties.getAuthorization() != null && !properties.getAuthorization().isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, properties.getAuthorization());
        }
        if (properties.getSessionId() != null && !properties.getSessionId().isBlank()) {
            headers.set(SESSION_ID_HEADER, properties.getSessionId());
        }
        return headers;
    }

    private Map<String, Object> toolPayload(String toolId, Map<String, Object> params, String requestId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", requestId);
        payload.put("toolId", toolId);
        payload.put("params", params == null ? Map.of() : params);
        return payload;
    }

    private Map<String, Object> castMap(Map<?, ?> raw) {
        Map<String, Object> mapped = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() instanceof String key) {
                mapped.put(key, entry.getValue());
            }
        }
        return mapped;
    }

    private String generatedRequestId() {
        return "mstest-" + System.currentTimeMillis();
    }

    private String resolveRequestId(String correlationId) {
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId;
        }
        String requestId = CorrelationContext.getRequestId();
        if (requestId == null || requestId.isBlank()) {
            return generatedRequestId();
        }
        return requestId;
    }

    private void sleep(long retryDelayMs) {
        if (retryDelayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(retryDelayMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}
