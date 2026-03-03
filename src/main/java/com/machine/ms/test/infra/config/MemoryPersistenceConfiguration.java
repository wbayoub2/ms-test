package com.machine.ms.test.infra.config;

import com.machine.ms.test.core.port.out.CoverageStore;
import com.machine.ms.test.core.port.out.DiffSnapshotStore;
import com.machine.ms.test.core.port.out.GateReportStore;
import com.machine.ms.test.core.port.out.ImpactSnapshotStore;
import com.machine.ms.test.core.port.out.ArtifactLinkStore;
import com.machine.ms.test.core.port.out.CommitChangeStore;
import com.machine.ms.test.core.port.out.TestRunStore;
import com.machine.ms.test.infra.persistence.InMemoryArtifactLinkStore;
import com.machine.ms.test.infra.persistence.InMemoryCommitChangeStore;
import com.machine.ms.test.infra.persistence.InMemoryCoverageStore;
import com.machine.ms.test.infra.persistence.InMemoryDiffSnapshotStore;
import com.machine.ms.test.infra.persistence.InMemoryGateReportStore;
import com.machine.ms.test.infra.persistence.InMemoryImpactSnapshotStore;
import com.machine.ms.test.infra.persistence.InMemoryTestRunStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "mstest.persistence", name = "mode", havingValue = "MEMORY", matchIfMissing = true)
public class MemoryPersistenceConfiguration {

    @Bean
    public DiffSnapshotStore diffSnapshotStore() {
        return new InMemoryDiffSnapshotStore();
    }

    @Bean
    public ImpactSnapshotStore impactSnapshotStore() {
        return new InMemoryImpactSnapshotStore();
    }

    @Bean
    public GateReportStore gateReportStore() {
        return new InMemoryGateReportStore();
    }

    @Bean
    public CoverageStore coverageStore() {
        return new InMemoryCoverageStore();
    }

    @Bean
    public TestRunStore testRunStore() {
        return new InMemoryTestRunStore();
    }

    @Bean
    public CommitChangeStore commitChangeStore() {
        return new InMemoryCommitChangeStore();
    }

    @Bean
    public ArtifactLinkStore artifactLinkStore() {
        return new InMemoryArtifactLinkStore();
    }
}
