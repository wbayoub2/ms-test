package com.machine.ms.test.infra.persistence.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.machine.ms.test.core.domain.model.CoverageSnapshot;
import com.machine.ms.test.core.port.out.CoverageStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class JdbcCoverageStore implements CoverageStore {

    private final JdbcTemplate jdbcTemplate;
    private final PostgresJsonMapper jsonMapper;

    public JdbcCoverageStore(JdbcTemplate jdbcTemplate, PostgresJsonMapper jsonMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public CoverageSnapshot save(CoverageSnapshot snapshot) {
        String sql = """
                INSERT INTO ms_test_coverage_snapshots(commit_sha, ingestion_id, request_id, service, repository, branch, tests_json, ingested_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(commit_sha)
                DO UPDATE SET ingestion_id = EXCLUDED.ingestion_id,
                              request_id = EXCLUDED.request_id,
                              service = EXCLUDED.service,
                              repository = EXCLUDED.repository,
                              branch = EXCLUDED.branch,
                              tests_json = EXCLUDED.tests_json,
                              ingested_at = EXCLUDED.ingested_at
                """;
        jdbcTemplate.update(sql,
                snapshot.commitSha(),
                snapshot.ingestionId(),
                snapshot.requestId(),
                snapshot.service(),
                snapshot.repository(),
                snapshot.branch(),
                jsonMapper.write(snapshot.tests()),
                Timestamp.from(snapshot.ingestedAt()));
        return snapshot;
    }

    @Override
    public Optional<CoverageSnapshot> findByCommitSha(String commitSha) {
        String sql = """
                SELECT commit_sha, ingestion_id, request_id, service, repository, branch, tests_json, ingested_at
                FROM ms_test_coverage_snapshots
                WHERE commit_sha = ?
                """;
        return mapRows(sql, commitSha).stream().findFirst();
    }

    @Override
    public List<CoverageSnapshot> findByServiceAndBranch(String service, String branch) {
        String sql = """
                SELECT commit_sha, ingestion_id, request_id, service, repository, branch, tests_json, ingested_at
                FROM ms_test_coverage_snapshots
                WHERE service = ? AND branch = ?
                ORDER BY ingested_at DESC
                """;
        return mapRows(sql, service, branch);
    }

    @Override
    public List<CoverageSnapshot> findAll() {
        String sql = """
                SELECT commit_sha, ingestion_id, request_id, service, repository, branch, tests_json, ingested_at
                FROM ms_test_coverage_snapshots
                ORDER BY ingested_at DESC
                """;
        return mapRows(sql);
    }

    private List<CoverageSnapshot> mapRows(String sql, Object... args) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> new CoverageSnapshot(
                rs.getString("ingestion_id"),
                rs.getString("request_id"),
                rs.getString("service"),
                rs.getString("repository"),
                rs.getString("branch"),
                rs.getString("commit_sha"),
                jsonMapper.read(rs.getString("tests_json"), new TypeReference<>() {
                }),
                rs.getTimestamp("ingested_at").toInstant()),
                args);
    }
}
