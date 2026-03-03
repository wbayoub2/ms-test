package com.machine.ms.test.core.domain.model;

public record CoverageLine(String filePath,
                           String className,
                           String methodSig,
                           int lineNo,
                           boolean covered) {
}
