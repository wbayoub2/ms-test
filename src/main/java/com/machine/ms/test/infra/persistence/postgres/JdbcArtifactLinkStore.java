package com.machine.ms.test.infra.persistence.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.machine.ms.test.core.domain.model.ArtifactLinkSnapshot;
import com.machine.ms.test.core.port.out.ArtifactLinkStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class JdbcArtifactLinkStore implements ArtifactLinkStore {

    private final JdbcTemplate jdbcTemplate;
    private final PostgresJsonMapper jsonMapper;

    public JdbcArtifactLinkStore(JdbcTemplate jdbcTemplate, PostgresJsonMapper jsonMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public ArtifactLinkSnapshot save(ArtifactLinkSnapshot snapshot) {
        String sql = """
                INSERT INTO ms_test_artifact_links(run_id, ingestion_id, request_id, artifacts_json, ingested_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(run_id)
                DO UPDATE SET ingestion_id = EXCLUDED.ingestion_id,
                              request_id = EXCLUDED.request_id,
                              artifacts_json = EXCLUDED.artifacts_json,
                              ingested_at = EXCLUDED.ingested_at
                """;
        jdbcTemplate.update(sql,
                snapshot.runId(),
                snapshot.ingestionId(),
                snapshot.requestId(),
                jsonMapper.write(snapshot.artifacts()),
                Timestamp.from(snapshot.ingestedAt()));
        return snapshot;
    }

    @Override
    public Optional<ArtifactLinkSnapshot> findByRunId(String runId) {
        String sql = """
                SELECT ingestion_id, request_id, artifacts_json, ingested_at
                FROM ms_test_artifact_links
                WHERE run_id = ?
                """;
        List<ArtifactLinkSnapshot> rows = jdbcTemplate.query(sql, (rs, rowNum) -> new ArtifactLinkSnapshot(
                rs.getString("ingestion_id"),
                rs.getString("request_id"),
                runId,
                jsonMapper.read(rs.getString("artifacts_json"), new TypeReference<>() {
                }),
                rs.getTimestamp("ingested_at").toInstant()),
                runId);
        return rows.stream().findFirst();
    }
}
