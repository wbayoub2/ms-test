package com.machine.ms.test.infra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.machine.ms.test.core.port.out.CoverageStore;
import com.machine.ms.test.core.port.out.DiffSnapshotStore;
import com.machine.ms.test.core.port.out.GateReportStore;
import com.machine.ms.test.core.port.out.ImpactSnapshotStore;
import com.machine.ms.test.core.port.out.ArtifactLinkStore;
import com.machine.ms.test.core.port.out.CommitChangeStore;
import com.machine.ms.test.core.port.out.TestRunStore;
import com.machine.ms.test.infra.persistence.postgres.JdbcArtifactLinkStore;
import com.machine.ms.test.infra.persistence.postgres.JdbcCommitChangeStore;
import com.machine.ms.test.infra.persistence.postgres.JdbcCoverageStore;
import com.machine.ms.test.infra.persistence.postgres.JdbcDiffSnapshotStore;
import com.machine.ms.test.infra.persistence.postgres.JdbcGateReportStore;
import com.machine.ms.test.infra.persistence.postgres.JdbcImpactSnapshotStore;
import com.machine.ms.test.infra.persistence.postgres.JdbcTestRunStore;
import com.machine.ms.test.infra.persistence.postgres.PostgresJsonMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnProperty(prefix = "mstest.persistence", name = "mode", havingValue = "POSTGRES")
public class PostgresPersistenceConfiguration {

    @Bean
    public PostgresJsonMapper postgresJsonMapper(ObjectMapper objectMapper) {
        return new PostgresJsonMapper(objectMapper);
    }

    @Bean
    public DiffSnapshotStore diffSnapshotStore(JdbcTemplate jdbcTemplate, PostgresJsonMapper jsonMapper) {
        return new JdbcDiffSnapshotStore(jdbcTemplate, jsonMapper);
    }

    @Bean
    public ImpactSnapshotStore impactSnapshotStore(JdbcTemplate jdbcTemplate, PostgresJsonMapper jsonMapper) {
        return new JdbcImpactSnapshotStore(jdbcTemplate, jsonMapper);
    }

    @Bean
    public GateReportStore gateReportStore(JdbcTemplate jdbcTemplate, PostgresJsonMapper jsonMapper) {
        return new JdbcGateReportStore(jdbcTemplate, jsonMapper);
    }

    @Bean
    public CoverageStore coverageStore(JdbcTemplate jdbcTemplate, PostgresJsonMapper jsonMapper) {
        return new JdbcCoverageStore(jdbcTemplate, jsonMapper);
    }

    @Bean
    public TestRunStore testRunStore(JdbcTemplate jdbcTemplate, PostgresJsonMapper jsonMapper) {
        return new JdbcTestRunStore(jdbcTemplate, jsonMapper);
    }

    @Bean
    public CommitChangeStore commitChangeStore(JdbcTemplate jdbcTemplate, PostgresJsonMapper jsonMapper) {
        return new JdbcCommitChangeStore(jdbcTemplate, jsonMapper);
    }

    @Bean
    public ArtifactLinkStore artifactLinkStore(JdbcTemplate jdbcTemplate, PostgresJsonMapper jsonMapper) {
        return new JdbcArtifactLinkStore(jdbcTemplate, jsonMapper);
    }
}
