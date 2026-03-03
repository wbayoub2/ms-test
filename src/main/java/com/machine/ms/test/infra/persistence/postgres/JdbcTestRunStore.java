package com.machine.ms.test.infra.persistence.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.machine.ms.test.core.domain.model.TestRunSnapshot;
import com.machine.ms.test.core.port.out.TestRunStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class JdbcTestRunStore implements TestRunStore {

    private final JdbcTemplate jdbcTemplate;
    private final PostgresJsonMapper jsonMapper;

    public JdbcTestRunStore(JdbcTemplate jdbcTemplate, PostgresJsonMapper jsonMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public TestRunSnapshot save(TestRunSnapshot snapshot) {
        String sql = """
                INSERT INTO ms_test_run_snapshots(run_id, ingestion_id, request_id, service, branch, commit_sha, launch_id, status, results_json, ingested_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(run_id)
                DO UPDATE SET ingestion_id = EXCLUDED.ingestion_id,
                              request_id = EXCLUDED.request_id,
                              service = EXCLUDED.service,
                              branch = EXCLUDED.branch,
                              commit_sha = EXCLUDED.commit_sha,
                              launch_id = EXCLUDED.launch_id,
                              status = EXCLUDED.status,
                              results_json = EXCLUDED.results_json,
                              ingested_at = EXCLUDED.ingested_at
                """;
        jdbcTemplate.update(sql,
                snapshot.runId(),
                snapshot.ingestionId(),
                snapshot.requestId(),
                snapshot.service(),
                snapshot.branch(),
                snapshot.commitSha(),
                snapshot.launchId(),
                snapshot.status(),
                jsonMapper.write(snapshot.results()),
                Timestamp.from(snapshot.ingestedAt()));
        return snapshot;
    }

    @Override
    public Optional<TestRunSnapshot> findByRunId(String runId) {
        String sql = """
                SELECT ingestion_id, request_id, service, branch, commit_sha, launch_id, status, results_json, ingested_at
                FROM ms_test_run_snapshots
                WHERE run_id = ?
                """;
        return mapRows(sql, runId).stream().findFirst();
    }

    @Override
    public List<TestRunSnapshot> findByServiceAndBranch(String service, String branch) {
        String sql = """
                SELECT run_id, ingestion_id, request_id, service, branch, commit_sha, launch_id, status, results_json, ingested_at
                FROM ms_test_run_snapshots
                WHERE service = ? AND branch = ?
                ORDER BY ingested_at DESC
                """;
        return mapRows(sql, service, branch);
    }

    @Override
    public List<TestRunSnapshot> findAll() {
        String sql = """
                SELECT run_id, ingestion_id, request_id, service, branch, commit_sha, launch_id, status, results_json, ingested_at
                FROM ms_test_run_snapshots
                ORDER BY ingested_at DESC
                """;
        return mapRows(sql);
    }

    private List<TestRunSnapshot> mapRows(String sql, Object... args) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> new TestRunSnapshot(
                rs.getString("ingestion_id"),
                rs.getString("request_id"),
                rs.getString("service"),
                rs.getString("branch"),
                rs.getString("commit_sha"),
                rs.getString("run_id"),
                rs.getString("launch_id"),
                rs.getString("status"),
                jsonMapper.read(rs.getString("results_json"), new TypeReference<>() {
                }),
                rs.getTimestamp("ingested_at").toInstant()),
                args);
    }
}
