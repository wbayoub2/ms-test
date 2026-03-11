package com.machine.ms.test.infra.integration;

import com.machine.common.api.context.CorrelationIdsDto;
import com.machine.common.starter.correlation.CorrelationContext;
import com.machine.common.starter.correlation.CorrelationContextHolder;
import com.machine.common.starter.http.CommonRestTemplateCustomizer;
import com.machine.ms.test.core.domain.error.MsTestErrorCode;
import com.machine.ms.test.core.domain.error.MsTestException;
import com.machine.ms.test.core.port.out.MsInfraToolPort;
import com.machine.ms.test.infra.config.MsInfraClientProperties;
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

    public MsInfraHttpToolClient(MsInfraClientProperties properties,
                                RestTemplateBuilder restTemplateBuilder,
                                CommonRestTemplateCustomizer commonRestTemplateCustomizer) {
        this.properties = properties;
        RestTemplateBuilder tunedBuilder = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        this.restTemplate = commonRestTemplateCustomizer.create(tunedBuilder);
    }

    @Override
    public Map<String, Object> startTestRun(Map<String, Object> payload, String correlationId) {
        return callWithRetry("/api/v1/e2e/runs", payload, correlationId, true);
    }

    @Override
    public Map<String, Object> getTestRun(String runId, String correlationId) {
        return callWithRetry("/api/v1/e2e/runs/" + runId, Map.of(), correlationId, false);
    }

    @Override
    public Map<String, Object> getTestRunArtifacts(String runId, String correlationId) {
        return callWithRetry("/api/v1/e2e/runs/" + runId + "/artifacts", Map.of(), correlationId, false);
    }

    private Map<String, Object> callWithRetry(String path, Map<String, Object> payload, String correlationId, boolean post) {
        if (!properties.isEnabled()) {
            throw new MsTestException(MsTestErrorCode.UPSTREAM_UNAVAILABLE, "ms-infra integration disabled");
        }
        int attempts = Math.max(1, properties.getMaxAttempts());
        RuntimeException lastFailure = null;
        String requestId = resolveRequestId(correlationId);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return post ? postJson(path, payload, headers(requestId)) : getJson(path, headers(requestId));
            } catch (RestClientException restClientException) {
                lastFailure = restClientException;
                sleep(properties.getRetryDelayMs());
            }
        }
        throw new MsTestException(MsTestErrorCode.UPSTREAM_UNAVAILABLE,
                "ms-infra call failed after retries",
                Map.of("path", path, "reason", lastFailure == null ? "unknown" : lastFailure.getMessage()));
    }

    private Map<String, Object> postJson(String path, Map<String, Object> payload, HttpHeaders headers) {
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        Map<?, ?> raw = restTemplate.postForObject(properties.getBaseUrl() + path, request, Map.class);
        if (raw == null) {
            throw new MsTestException(MsTestErrorCode.UPSTREAM_UNAVAILABLE, "ms-infra returned empty response");
        }
        return castMap(raw);
    }

    private Map<String, Object> getJson(String path, HttpHeaders headers) {
        HttpEntity<Void> request = new HttpEntity<>(headers);
        Map<?, ?> raw = restTemplate.exchange(properties.getBaseUrl() + path, org.springframework.http.HttpMethod.GET, request, Map.class).getBody();
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
        CorrelationIdsDto correlationIds = resolveCorrelationIds();
        String requestId = correlationIds.requestId();
        if (requestId == null || requestId.isBlank()) {
            return generatedRequestId();
        }
        return requestId;
    }

    private CorrelationIdsDto resolveCorrelationIds() {
        CorrelationContext context = CorrelationContextHolder.get();
        if (context == null) {
            return new CorrelationIdsDto(null, null, null, null, null);
        }
        return new CorrelationIdsDto(context.requestId(), context.sessionId(), context.instanceId(), null, null);
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
