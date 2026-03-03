package com.machine.ms.test.infra.persistence.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.machine.ms.test.core.domain.model.ImpactSnapshot;
import com.machine.ms.test.core.domain.model.StepKey;
import com.machine.ms.test.core.port.out.ImpactSnapshotStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class JdbcImpactSnapshotStore implements ImpactSnapshotStore {

    private final JdbcTemplate jdbcTemplate;
    private final PostgresJsonMapper jsonMapper;

    public JdbcImpactSnapshotStore(JdbcTemplate jdbcTemplate, PostgresJsonMapper jsonMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Optional<ImpactSnapshot> findByStepKey(StepKey key) {
        String sql = """
                SELECT impact_id, tests_json, generated_at, request_id
                FROM ms_test_impact_snapshots
                WHERE step_id = ? AND phase = ? AND base_sha = ? AND head_sha = ?
                """;
        List<ImpactSnapshot> rows = jdbcTemplate.query(sql, (rs, rowNum) -> new ImpactSnapshot(
                        rs.getString("impact_id"),
                        key,
                        jsonMapper.read(rs.getString("tests_json"), new TypeReference<>() {
                        }),
                        rs.getTimestamp("generated_at").toInstant(),
                        rs.getString("request_id")),
                key.stepId(), key.phase().name(), key.baseSha(), key.headSha());
        return rows.stream().findFirst();
    }

    @Override
    public ImpactSnapshot save(ImpactSnapshot snapshot) {
        String sql = """
                INSERT INTO ms_test_impact_snapshots(step_id, phase, base_sha, head_sha, impact_id, tests_json, generated_at, request_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(step_id, phase, base_sha, head_sha)
                DO UPDATE SET impact_id = EXCLUDED.impact_id,
                              tests_json = EXCLUDED.tests_json,
                              generated_at = EXCLUDED.generated_at,
                              request_id = EXCLUDED.request_id
                """;
        StepKey key = snapshot.key();
        jdbcTemplate.update(sql,
                key.stepId(),
                key.phase().name(),
                key.baseSha(),
                key.headSha(),
                snapshot.impactId(),
                jsonMapper.write(snapshot.tests()),
                Timestamp.from(snapshot.generatedAt()),
                snapshot.requestId());
        return snapshot;
    }
}
