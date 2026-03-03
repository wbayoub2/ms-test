package com.machine.ms.test.infra.persistence.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.machine.ms.test.core.domain.model.CommitChangeSnapshot;
import com.machine.ms.test.core.port.out.CommitChangeStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class JdbcCommitChangeStore implements CommitChangeStore {

    private final JdbcTemplate jdbcTemplate;
    private final PostgresJsonMapper jsonMapper;

    public JdbcCommitChangeStore(JdbcTemplate jdbcTemplate, PostgresJsonMapper jsonMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public CommitChangeSnapshot save(CommitChangeSnapshot snapshot) {
        String sql = """
                INSERT INTO ms_test_commit_changes(service, head_commit, ingestion_id, request_id, branch, base_commit, changes_json, ingested_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(service, head_commit)
                DO UPDATE SET ingestion_id = EXCLUDED.ingestion_id,
                              request_id = EXCLUDED.request_id,
                              branch = EXCLUDED.branch,
                              base_commit = EXCLUDED.base_commit,
                              changes_json = EXCLUDED.changes_json,
                              ingested_at = EXCLUDED.ingested_at
                """;
        jdbcTemplate.update(sql,
                snapshot.service(),
                snapshot.headCommit(),
                snapshot.ingestionId(),
                snapshot.requestId(),
                snapshot.branch(),
                snapshot.baseCommit(),
                jsonMapper.write(snapshot.changes()),
                Timestamp.from(snapshot.ingestedAt()));
        return snapshot;
    }

    @Override
    public Optional<CommitChangeSnapshot> findByHeadCommit(String service, String headCommit) {
        String sql = """
                SELECT ingestion_id, request_id, branch, base_commit, changes_json, ingested_at
                FROM ms_test_commit_changes
                WHERE service = ? AND head_commit = ?
                """;
        List<CommitChangeSnapshot> rows = jdbcTemplate.query(sql, (rs, rowNum) -> new CommitChangeSnapshot(
                rs.getString("ingestion_id"),
                rs.getString("request_id"),
                service,
                rs.getString("branch"),
                rs.getString("base_commit"),
                headCommit,
                jsonMapper.read(rs.getString("changes_json"), new TypeReference<>() {
                }),
                rs.getTimestamp("ingested_at").toInstant()),
                service, headCommit);
        return rows.stream().findFirst();
    }
}
