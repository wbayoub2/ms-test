package com.machine.ms.test.api.http;

import com.machine.ms.test.api.contract.error.StandardMsTestErrorDto;
import com.machine.ms.test.core.domain.error.MsTestErrorCode;
import com.machine.ms.test.core.domain.error.MsTestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MsTestException.class)
    public ResponseEntity<StandardMsTestErrorDto> handleMsTestException(MsTestException exception) {
        HttpStatus status = switch (exception.code()) {
            case GATE_IDEMPOTENCY_CONFLICT -> HttpStatus.CONFLICT;
            case UNSUPPORTED_STEP_PHASE -> HttpStatus.UNPROCESSABLE_ENTITY;
            case UPSTREAM_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case SNAPSHOT_NOT_FOUND, RUN_NOT_FOUND, TEST_NOT_FOUND, COMMIT_NOT_FOUND, EVIDENCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case IMPACT_COMPUTE_FAILED, DIFF_DATA_UNAVAILABLE, IMPACT_DATA_UNAVAILABLE -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(new StandardMsTestErrorDto(
                exception.code().name(),
                exception.code().type(),
                exception.getMessage(),
                exception.context()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardMsTestErrorDto> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, Object> context = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            context.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(new StandardMsTestErrorDto(
                MsTestErrorCode.INVALID_INPUT.name(),
                MsTestErrorCode.INVALID_INPUT.type(),
                "Invalid request payload",
                context));
    }
}
