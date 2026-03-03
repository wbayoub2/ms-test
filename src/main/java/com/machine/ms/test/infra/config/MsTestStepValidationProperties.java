package com.machine.ms.test.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mstest.step-validation")
public class MsTestStepValidationProperties {

    private DiffPolicy diffPolicy = DiffPolicy.ALLOW_SYNTHETIC_FALLBACK;
    private ImpactPolicy impactPolicy = ImpactPolicy.ALLOW_DEFAULT_FALLBACK;

    public DiffPolicy getDiffPolicy() {
        return diffPolicy;
    }

    public void setDiffPolicy(DiffPolicy diffPolicy) {
        this.diffPolicy = diffPolicy == null ? DiffPolicy.ALLOW_SYNTHETIC_FALLBACK : diffPolicy;
    }

    public ImpactPolicy getImpactPolicy() {
        return impactPolicy;
    }

    public void setImpactPolicy(ImpactPolicy impactPolicy) {
        this.impactPolicy = impactPolicy == null ? ImpactPolicy.ALLOW_DEFAULT_FALLBACK : impactPolicy;
    }

    public enum DiffPolicy {
        STRICT,
        ALLOW_SYNTHETIC_FALLBACK
    }

    public enum ImpactPolicy {
        STRICT,
        ALLOW_DEFAULT_FALLBACK
    }
}
