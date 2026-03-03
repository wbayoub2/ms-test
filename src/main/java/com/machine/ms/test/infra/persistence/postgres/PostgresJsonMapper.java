package com.machine.ms.test.infra.persistence.postgres;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.machine.ms.test.core.domain.error.MsTestErrorCode;
import com.machine.ms.test.core.domain.error.MsTestException;

public class PostgresJsonMapper {

    private final ObjectMapper objectMapper;

    public PostgresJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T read(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new MsTestException(MsTestErrorCode.INVALID_INPUT, "Cannot decode persisted json");
        }
    }

    public String write(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new MsTestException(MsTestErrorCode.INVALID_INPUT, "Cannot encode persisted json");
        }
    }
}
