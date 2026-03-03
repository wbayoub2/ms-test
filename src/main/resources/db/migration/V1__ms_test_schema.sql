CREATE TABLE IF NOT EXISTS ms_test_diff_snapshots (
    step_id VARCHAR(120) NOT NULL,
    phase VARCHAR(20) NOT NULL,
    base_sha VARCHAR(64) NOT NULL,
    head_sha VARCHAR(64) NOT NULL,
    diff_id VARCHAR(120) NOT NULL,
    changed_files_json TEXT NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL,
    request_id VARCHAR(120) NOT NULL,
    PRIMARY KEY (step_id, phase, base_sha, head_sha)
);

CREATE TABLE IF NOT EXISTS ms_test_impact_snapshots (
    step_id VARCHAR(120) NOT NULL,
    phase VARCHAR(20) NOT NULL,
    base_sha VARCHAR(64) NOT NULL,
    head_sha VARCHAR(64) NOT NULL,
    impact_id VARCHAR(120) NOT NULL,
    tests_json TEXT NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL,
    request_id VARCHAR(120) NOT NULL,
    PRIMARY KEY (step_id, phase, base_sha, head_sha)
);

CREATE TABLE IF NOT EXISTS ms_test_gate_reports (
    step_id VARCHAR(120) NOT NULL,
    phase VARCHAR(20) NOT NULL,
    base_sha VARCHAR(64) NOT NULL,
    head_sha VARCHAR(64) NOT NULL,
    policy_id VARCHAR(120) NOT NULL,
    run_id VARCHAR(120) NOT NULL,
    gate_report_id VARCHAR(120) NOT NULL,
    request_id VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reasons_json TEXT NOT NULL,
    evaluated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (step_id, phase, base_sha, head_sha, policy_id, run_id),
    CONSTRAINT uq_ms_test_gate_request UNIQUE (request_id)
);

CREATE TABLE IF NOT EXISTS ms_test_coverage_snapshots (
    commit_sha VARCHAR(64) PRIMARY KEY,
    ingestion_id VARCHAR(120) NOT NULL,
    request_id VARCHAR(120) NOT NULL,
    service VARCHAR(120) NOT NULL,
    repository VARCHAR(255) NOT NULL,
    branch VARCHAR(120) NOT NULL,
    tests_json TEXT NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS ms_test_run_snapshots (
    run_id VARCHAR(120) PRIMARY KEY,
    ingestion_id VARCHAR(120) NOT NULL,
    request_id VARCHAR(120) NOT NULL,
    service VARCHAR(120) NOT NULL,
    branch VARCHAR(120) NOT NULL,
    commit_sha VARCHAR(64) NOT NULL,
    launch_id VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    results_json TEXT NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ms_test_gate_request ON ms_test_gate_reports(request_id);
CREATE INDEX IF NOT EXISTS idx_ms_test_diff_request ON ms_test_diff_snapshots(request_id);
CREATE INDEX IF NOT EXISTS idx_ms_test_impact_request ON ms_test_impact_snapshots(request_id);
CREATE INDEX IF NOT EXISTS idx_ms_test_coverage_request ON ms_test_coverage_snapshots(request_id);
CREATE INDEX IF NOT EXISTS idx_ms_test_run_request ON ms_test_run_snapshots(request_id);
