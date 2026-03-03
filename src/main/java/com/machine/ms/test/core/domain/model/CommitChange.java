package com.machine.ms.test.core.domain.model;

public record CommitChange(String filePath,
                           String methodSig,
                           int lineStart,
                           int lineEnd,
                           String changeType) {
}
