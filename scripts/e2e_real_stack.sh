#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
PROJECT_NAME="${MS_TEST_COMPOSE_PROJECT:-ms-test-e2e}"
COMPOSE_CMD=(docker compose -p "$PROJECT_NAME")
MS_INFRA_BASE_URL="${MS_INFRA_BASE_URL:-http://127.0.0.1:18081}"
MS_INFRA_HEALTH_URL="${MS_INFRA_HEALTH_URL:-${MS_INFRA_BASE_URL}/tools/catalog}"
MS_INFRA_E2E_REQUIRE_SUCCESS="${MS_INFRA_E2E_REQUIRE_SUCCESS:-false}"
MS_INFRA_CONTAINER_BASE_URL="${MS_INFRA_CONTAINER_BASE_URL:-$MS_INFRA_BASE_URL}"

if [[ "$MS_INFRA_CONTAINER_BASE_URL" == http://127.0.0.1:* ]]; then
  MS_INFRA_CONTAINER_BASE_URL="${MS_INFRA_CONTAINER_BASE_URL/http:\/\/127.0.0.1/http:\/\/host.docker.internal}"
fi
if [[ "$MS_INFRA_CONTAINER_BASE_URL" == http://localhost:* ]]; then
  MS_INFRA_CONTAINER_BASE_URL="${MS_INFRA_CONTAINER_BASE_URL/http:\/\/localhost/http:\/\/host.docker.internal}"
fi

if [[ "${MS_INFRA_E2E_ENABLED:-false}" == "true" ]]; then
  export MS_TEST_MS_INFRA_ENABLED=true
  export MS_TEST_MS_INFRA_BASE_URL="$MS_INFRA_CONTAINER_BASE_URL"
  export MS_TEST_MS_INFRA_COOKIE="${MS_TEST_MS_INFRA_COOKIE:-}"
  export MS_TEST_MS_INFRA_AUTHORIZATION="${MS_TEST_MS_INFRA_AUTHORIZATION:-}"
  export MS_TEST_MS_INFRA_SESSION_ID="${MS_TEST_MS_INFRA_SESSION_ID:-}"
fi

cleanup() {
  "${COMPOSE_CMD[@]}" down >/dev/null 2>&1 || true
}
trap cleanup EXIT

"${COMPOSE_CMD[@]}" up -d --build

for i in {1..45}; do
  if curl -sS --max-time 2 http://127.0.0.1:18092/actuator/health 2>/dev/null | rg -q '"status":"UP"'; then
    break
  fi
  sleep 2
done

if ! curl -sS --max-time 2 http://127.0.0.1:18092/actuator/health 2>/dev/null | rg -q '"status":"UP"'; then
  echo "ms-test app did not become healthy" >&2
  exit 1
fi

cat >/tmp/ms-test-cov.json <<'JSON'
{"requestId":"req-e2e-cov","service":"ms-plan","repository":"github.com/org/ms-plan","branch":"main","commitSha":"ff89aa10","tests":[{"testId":"com.machine.ms.FooServiceTest.shouldCompute","coverage":[{"filePath":"src/main/java/com/machine/ms/Foo.java","className":"Foo","methodSig":"compute()","lineNo":21,"covered":false}]}]}
JSON
curl -sS -X POST http://127.0.0.1:18092/api/v1/ingestion/openclover/testwise-coverage -H 'Content-Type: application/json' --data-binary @/tmp/ms-test-cov.json >/tmp/ms-test-cov.out

cat >/tmp/ms-test-run.json <<'JSON'
{"requestId":"req-e2e-run","service":"ms-plan","branch":"main","commitSha":"ff89aa10","runId":"run_e2e","launchId":"launch_e2e","status":"FAILED","results":[{"testId":"com.machine.ms.FooServiceTest.shouldCompute","status":"FAILED","durationMs":120,"errorSignature":"assert"}]}
JSON
curl -sS -X POST http://127.0.0.1:18092/api/v1/ingestion/reportportal/test-runs -H 'Content-Type: application/json' --data-binary @/tmp/ms-test-run.json >/tmp/ms-test-run.out

cat >/tmp/ms-test-changes.json <<'JSON'
{"requestId":"req-e2e-chg","service":"ms-plan","branch":"main","baseCommit":"ab12cd34","headCommit":"ff89aa10","changes":[{"filePath":"src/main/java/com/machine/ms/Foo.java","methodSig":"compute()","lineStart":10,"lineEnd":40,"changeType":"MODIFIED"}]}
JSON
curl -sS -X POST http://127.0.0.1:18092/api/v1/ingestion/commit-changes -H 'Content-Type: application/json' --data-binary @/tmp/ms-test-changes.json >/tmp/ms-test-changes.out

cat >/tmp/ms-test-art.json <<'JSON'
{"requestId":"req-e2e-art","runId":"run_e2e","artifacts":[{"artifactType":"CLOVER_XML","storage":"MINIO","url":"s3://qa-evidence/run_e2e/clover.xml","checksum":"abc"}]}
JSON
curl -sS -X POST http://127.0.0.1:18092/api/v1/ingestion/artifacts -H 'Content-Type: application/json' --data-binary @/tmp/ms-test-art.json >/tmp/ms-test-art.out

curl -sS "http://127.0.0.1:18092/api/v1/tests/com.machine.ms.FooServiceTest.shouldCompute/history?service=ms-plan&branch=main&limit=5" | rg -q 'history'
curl -sS "http://127.0.0.1:18092/api/v1/impact/compute-from-commits" -X POST -H 'Content-Type: application/json' --data '{"service":"ms-plan","baseCommit":"ab12cd34","headCommit":"ff89aa10","branch":"main"}' | rg -q 'impactedTests'
curl -sS "http://127.0.0.1:18092/api/v1/runs/run_e2e/evidence" | rg -q 'CLOVER_XML'
curl -sS "http://127.0.0.1:18092/api/v1/quality-gates/policies" | rg -q 'validation-policy-v1'

TABLE_COUNT=$("${COMPOSE_CMD[@]}" exec -T postgres psql -U ms_test -d ms_test -Atc "select count(*) from ms_test_run_snapshots;")
if [[ "$TABLE_COUNT" -lt 1 ]]; then
  echo "expected persisted runs in postgres" >&2
  exit 1
fi

if [[ "${MS_INFRA_E2E_ENABLED:-false}" == "true" ]]; then
  curl -sS -i --max-time 5 "$MS_INFRA_HEALTH_URL" | rg -q '200'
  INFRA_RESPONSE="$(curl -sS -X POST http://127.0.0.1:18092/api/v1/integration/ms-infra/test-run/start-and-poll \
    -H 'Content-Type: application/json' \
    --data '{"workflowId":"quality.yml","inputs":{"selectionMode":"IMPACTED_ONLY","tests":["com.machine.ms.FooServiceTest.shouldCompute"]},"pollAttempts":1,"pollSleepMs":200}' \
  )"
  if [[ "$MS_INFRA_E2E_REQUIRE_SUCCESS" == "true" ]]; then
    echo "$INFRA_RESPONSE" | rg -q 'runId'
  else
    echo "$INFRA_RESPONSE" | rg -q 'runId|errorCode'
  fi
fi

echo "E2E real stack: OK"
