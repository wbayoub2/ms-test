package com.machine.ms.test.api.bridge;

import com.machine.ms.test.core.domain.error.MsTestErrorCode;
import com.machine.ms.test.core.domain.error.MsTestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = MsTestToolBridgeController.class)
public class MsTestToolBridgeExceptionHandler {

    @ExceptionHandler(BridgeInvalidParametersException.class)
    public ResponseEntity<BridgeErrorDto> handleInvalidParameters(BridgeInvalidParametersException exception) {
        return ResponseEntity.badRequest().body(new BridgeErrorDto(
                "INVALID_PARAMETERS",
                com.machine.ms.test.core.domain.error.MsTestErrorType.VALIDATION,
                exception.getMessage(),
                false,
                "MS_TEST",
                exception.context()));
    }

    @ExceptionHandler(MsTestException.class)
    public ResponseEntity<BridgeErrorDto> handleMsTestException(MsTestException exception) {
        HttpStatus status = switch (exception.code()) {
            case GATE_IDEMPOTENCY_CONFLICT -> HttpStatus.CONFLICT;
            case UNSUPPORTED_STEP_PHASE -> HttpStatus.UNPROCESSABLE_ENTITY;
            case UPSTREAM_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case SNAPSHOT_NOT_FOUND, RUN_NOT_FOUND, TEST_NOT_FOUND, COMMIT_NOT_FOUND, EVIDENCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case IMPACT_COMPUTE_FAILED, DIFF_DATA_UNAVAILABLE, IMPACT_DATA_UNAVAILABLE -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(new BridgeErrorDto(
                exception.code().name(),
                exception.code().type(),
                exception.getMessage(),
                isRetriable(exception.code()),
                "MS_TEST",
                exception.context()));
    }

    private boolean isRetriable(MsTestErrorCode errorCode) {
        return errorCode == MsTestErrorCode.UPSTREAM_UNAVAILABLE;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BridgeErrorDto> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new BridgeErrorDto(
                "BACKEND_UNCLASSIFIED_ERROR",
                com.machine.ms.test.core.domain.error.MsTestErrorType.PROVIDER,
                exception.getMessage() == null ? "unexpected error" : exception.getMessage(),
                true,
                "MS_TEST",
                Map.of()));
    }
}
