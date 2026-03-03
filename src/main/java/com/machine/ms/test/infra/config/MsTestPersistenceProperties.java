package com.machine.ms.test.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mstest.persistence")
public class MsTestPersistenceProperties {

    private PersistenceMode mode = PersistenceMode.MEMORY;

    public PersistenceMode getMode() {
        return mode;
    }

    public void setMode(PersistenceMode mode) {
        this.mode = mode;
    }

    public boolean isPostgresMode() {
        return mode == PersistenceMode.POSTGRES;
    }
}
