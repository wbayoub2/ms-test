package com.machine.ms.test.infra.persistence.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.machine.ms.test.core.domain.model.DiffSnapshot;
import com.machine.ms.test.core.domain.model.StepKey;
import com.machine.ms.test.core.domain.model.StepPhase;
import com.machine.ms.test.core.port.out.DiffSnapshotStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class JdbcDiffSnapshotStore implements DiffSnapshotStore {

    private final JdbcTemplate jdbcTemplate;
    private final PostgresJsonMapper jsonMapper;

    public JdbcDiffSnapshotStore(JdbcTemplate jdbcTemplate, PostgresJsonMapper jsonMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Optional<DiffSnapshot> findByStepKey(StepKey key) {
        String sql = """
                SELECT diff_id, changed_files_json, generated_at, request_id
                FROM ms_test_diff_snapshots
                WHERE step_id = ? AND phase = ? AND base_sha = ? AND head_sha = ?
                """;
        List<DiffSnapshot> rows = jdbcTemplate.query(sql, (rs, rowNum) -> new DiffSnapshot(
                        rs.getString("diff_id"),
                        key,
                        jsonMapper.read(rs.getString("changed_files_json"), new TypeReference<>() {
                        }),
                        rs.getTimestamp("generated_at").toInstant(),
                        rs.getString("request_id")),
                key.stepId(), key.phase().name(), key.baseSha(), key.headSha());
        return rows.stream().findFirst();
    }

    @Override
    public DiffSnapshot save(DiffSnapshot snapshot) {
        String sql = """
                INSERT INTO ms_test_diff_snapshots(step_id, phase, base_sha, head_sha, diff_id, changed_files_json, generated_at, request_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(step_id, phase, base_sha, head_sha)
                DO UPDATE SET diff_id = EXCLUDED.diff_id,
                              changed_files_json = EXCLUDED.changed_files_json,
                              generated_at = EXCLUDED.generated_at,
                              request_id = EXCLUDED.request_id
                """;
        StepKey key = snapshot.key();
        jdbcTemplate.update(sql,
                key.stepId(),
                key.phase().name(),
                key.baseSha(),
                key.headSha(),
                snapshot.diffId(),
                jsonMapper.write(snapshot.changedFiles()),
                Timestamp.from(snapshot.generatedAt()),
                snapshot.requestId());
        return snapshot;
    }
}
