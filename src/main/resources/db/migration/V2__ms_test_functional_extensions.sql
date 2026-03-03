CREATE TABLE IF NOT EXISTS ms_test_commit_changes (
    service VARCHAR(120) NOT NULL,
    head_commit VARCHAR(64) NOT NULL,
    ingestion_id VARCHAR(120) NOT NULL,
    request_id VARCHAR(120) NOT NULL,
    branch VARCHAR(120) NOT NULL,
    base_commit VARCHAR(64) NOT NULL,
    changes_json TEXT NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (service, head_commit)
);

CREATE TABLE IF NOT EXISTS ms_test_artifact_links (
    run_id VARCHAR(120) PRIMARY KEY,
    ingestion_id VARCHAR(120) NOT NULL,
    request_id VARCHAR(120) NOT NULL,
    artifacts_json TEXT NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ms_test_commit_changes_request ON ms_test_commit_changes(request_id);
CREATE INDEX IF NOT EXISTS idx_ms_test_artifact_links_request ON ms_test_artifact_links(request_id);
