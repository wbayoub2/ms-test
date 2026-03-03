package com.machine.ms.test.api.contract.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.machine.ms.test.core.domain.error.MsTestErrorType;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StandardMsTestErrorDto(String errorCode,
                                     MsTestErrorType errorType,
                                     String message,
                                     Map<String, Object> context) {
}
