package com.machine.ms.test.core.domain.model;

public record ArtifactLink(String artifactType,
                           String storage,
                           String url,
                           String checksum) {
}
