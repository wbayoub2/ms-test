package com.machine.ms.test.infra.persistence.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.machine.ms.test.core.domain.model.GateReport;
import com.machine.ms.test.core.domain.model.GateStatus;
import com.machine.ms.test.core.domain.model.GateTupleKey;
import com.machine.ms.test.core.domain.model.StepPhase;
import com.machine.ms.test.core.port.out.GateReportStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class JdbcGateReportStore implements GateReportStore {

    private final JdbcTemplate jdbcTemplate;
    private final PostgresJsonMapper jsonMapper;

    public JdbcGateReportStore(JdbcTemplate jdbcTemplate, PostgresJsonMapper jsonMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Optional<GateReport> findByTupleKey(GateTupleKey key) {
        String sql = """
                SELECT gate_report_id, request_id, status, reasons_json, evaluated_at
                FROM ms_test_gate_reports
                WHERE step_id = ? AND phase = ? AND base_sha = ? AND head_sha = ? AND policy_id = ? AND run_id = ?
                """;
        List<GateReport> rows = jdbcTemplate.query(sql, (rs, rowNum) -> new GateReport(
                        rs.getString("gate_report_id"),
                        key,
                        rs.getString("request_id"),
                        GateStatus.valueOf(rs.getString("status")),
                        jsonMapper.read(rs.getString("reasons_json"), new TypeReference<>() {
                        }),
                        rs.getTimestamp("evaluated_at").toInstant()),
                key.stepId(), key.phase().name(), key.baseSha(), key.headSha(), key.policyId(), key.runId());
        return rows.stream().findFirst();
    }

    @Override
    public Optional<GateTupleKey> findTupleByRequestId(String requestId) {
        String sql = """
                SELECT step_id, phase, base_sha, head_sha, policy_id, run_id
                FROM ms_test_gate_reports
                WHERE request_id = ?
                """;
        List<GateTupleKey> rows = jdbcTemplate.query(sql, (rs, rowNum) -> new GateTupleKey(
                rs.getString("step_id"),
                StepPhase.valueOf(rs.getString("phase")),
                rs.getString("base_sha"),
                rs.getString("head_sha"),
                rs.getString("policy_id"),
                rs.getString("run_id")),
                requestId);
        return rows.stream().findFirst();
    }

    @Override
    public GateReport save(GateReport report) {
        String sql = """
                INSERT INTO ms_test_gate_reports(step_id, phase, base_sha, head_sha, policy_id, run_id, gate_report_id, request_id, status, reasons_json, evaluated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(step_id, phase, base_sha, head_sha, policy_id, run_id)
                DO UPDATE SET gate_report_id = EXCLUDED.gate_report_id,
                              request_id = EXCLUDED.request_id,
                              status = EXCLUDED.status,
                              reasons_json = EXCLUDED.reasons_json,
                              evaluated_at = EXCLUDED.evaluated_at
                """;
        GateTupleKey key = report.tupleKey();
        jdbcTemplate.update(sql,
                key.stepId(),
                key.phase().name(),
                key.baseSha(),
                key.headSha(),
                key.policyId(),
                key.runId(),
                report.gateReportId(),
                report.requestId(),
                report.status().name(),
                jsonMapper.write(report.reasons()),
                Timestamp.from(report.evaluatedAt()));
        return report;
    }
}
