package com.machine.ms.test.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mstest.integration")
public class MsTestIntegrationProperties {

    private IntegrationMode mode = IntegrationMode.BACKEND;
    private boolean includeAdvisoryText = false;

    public IntegrationMode getMode() {
        return mode;
    }

    public void setMode(IntegrationMode mode) {
        this.mode = mode;
    }

    public boolean isIncludeAdvisoryText() {
        return includeAdvisoryText;
    }

    public void setIncludeAdvisoryText(boolean includeAdvisoryText) {
        this.includeAdvisoryText = includeAdvisoryText;
    }

    public boolean isBackendMode() {
        return mode == IntegrationMode.BACKEND;
    }
}
