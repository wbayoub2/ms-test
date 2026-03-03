package com.machine.ms.test.api.bridge;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.machine.ms.test.core.domain.error.MsTestErrorType;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BridgeErrorDto(String errorCode,
                             MsTestErrorType errorType,
                             String message,
                             boolean retriable,
                             String provider,
                             Map<String, Object> context) {
}
