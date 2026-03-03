#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

PROJECT_NAME="${MS_TEST_COMPOSE_PROJECT:-ms-test-e2e-full}"
COMPOSE_CMD=(docker compose -p "$PROJECT_NAME")
RP_SCRIPT="$ROOT_DIR/scripts/reportportal_stack.sh"
RP_PROJECT="${MS_TEST_RP_PROJECT:-ms-test-rp}"
RP_BASE_URL="${MS_TEST_RP_BASE_URL:-http://127.0.0.1:28080}"
RP_TOKEN=""
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

rp_api() {
  local method="$1"
  local path="$2"
  local data="${3:-}"
  local response=""
  for _ in {1..30}; do
    if [[ -n "$data" ]]; then
      response="$(curl -fsS --max-time 10 -X "$method" "$RP_BASE_URL$path" \
        -H "Authorization: bearer $RP_TOKEN" \
        -H "Content-Type: application/json" \
        --data "$data" 2>/dev/null || true)"
    else
      response="$(curl -fsS --max-time 10 -X "$method" "$RP_BASE_URL$path" \
        -H "Authorization: bearer $RP_TOKEN" 2>/dev/null || true)"
    fi
    if [[ -n "$response" ]]; then
      echo "$response"
      return 0
    fi
    sleep 2
  done
  echo "ReportPortal API call failed: $method $path" >&2
  return 1
}

cleanup() {
  "${COMPOSE_CMD[@]}" down >/dev/null 2>&1 || true
  if [[ "${KEEP_REPORTPORTAL_UP:-false}" != "true" ]]; then
    MS_TEST_RP_PROJECT="$RP_PROJECT" "$RP_SCRIPT" stop >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

command -v curl >/dev/null || { echo "curl is required" >&2; exit 1; }
command -v jq >/dev/null || { echo "jq is required" >&2; exit 1; }

if [[ "${MS_INFRA_E2E_ENABLED:-false}" == "true" ]]; then
  export MS_TEST_MS_INFRA_ENABLED=true
  export MS_TEST_MS_INFRA_BASE_URL="$MS_INFRA_CONTAINER_BASE_URL"
  export MS_TEST_MS_INFRA_COOKIE="${MS_TEST_MS_INFRA_COOKIE:-}"
  export MS_TEST_MS_INFRA_AUTHORIZATION="${MS_TEST_MS_INFRA_AUTHORIZATION:-}"
  export MS_TEST_MS_INFRA_SESSION_ID="${MS_TEST_MS_INFRA_SESSION_ID:-}"
fi

MS_TEST_RP_PROJECT="$RP_PROJECT" "$RP_SCRIPT" start
RP_TOKEN="$(MS_TEST_RP_PROJECT="$RP_PROJECT" "$RP_SCRIPT" token)"

"${COMPOSE_CMD[@]}" up -d --build
for _ in {1..45}; do
  if curl -sS --max-time 2 http://127.0.0.1:18092/actuator/health | rg -q '"status":"UP"'; then
    break
  fi
  sleep 2
done

curl -sS --max-time 2 http://127.0.0.1:18092/actuator/health | rg -q '"status":"UP"' \
  || { echo "ms-test app did not become healthy" >&2; exit 1; }

NOW="$(date +%s%3N)"
LAUNCH_CREATE="$(rp_api POST "/api/v1/default_personal/launch" \
  "{\"name\":\"ms-test-full-e2e\",\"startTime\":\"$NOW\",\"mode\":\"DEFAULT\",\"description\":\"ms-test reportportal e2e\"}")"
LAUNCH_UUID="$(echo "$LAUNCH_CREATE" | jq -r '.id')"

ITEM_UUID="$(rp_api POST "/api/v1/default_personal/item" \
  "{\"launchUuid\":\"$LAUNCH_UUID\",\"name\":\"com.machine.ms.FooServiceTest.shouldCompute\",\"startTime\":\"$NOW\",\"type\":\"STEP\"}" \
  | jq -r '.id')"

END_TIME="$(date +%s%3N)"
rp_api PUT "/api/v1/default_personal/item/$ITEM_UUID" \
  "{\"endTime\":\"$END_TIME\",\"status\":\"FAILED\"}" >/dev/null

FINISH_LAUNCH="$(rp_api PUT "/api/v1/default_personal/launch/$LAUNCH_UUID/finish" \
  "{\"endTime\":\"$END_TIME\"}")"
LAUNCH_NUMBER="$(echo "$FINISH_LAUNCH" | jq -r '.number')"
LAUNCH_LINK="$(echo "$FINISH_LAUNCH" | jq -r '.link')"
RUN_ID="rp_run_${LAUNCH_NUMBER}"

cat >/tmp/ms-test-cov.json <<'JSON'
{"requestId":"req-full-cov","service":"ms-plan","repository":"github.com/org/ms-plan","branch":"main","commitSha":"ff89aa10","tests":[{"testId":"com.machine.ms.FooServiceTest.shouldCompute","coverage":[{"filePath":"src/main/java/com/machine/ms/Foo.java","className":"Foo","methodSig":"compute()","lineNo":21,"covered":false}]}]}
JSON
curl -fsS --max-time 10 -X POST http://127.0.0.1:18092/api/v1/ingestion/openclover/testwise-coverage \
  -H "Content-Type: application/json" --data-binary @/tmp/ms-test-cov.json >/dev/null

cat >/tmp/ms-test-run.json <<JSON
{"requestId":"req-full-run","service":"ms-plan","branch":"main","commitSha":"ff89aa10","runId":"$RUN_ID","launchId":"$LAUNCH_UUID","status":"FAILED","results":[{"testId":"com.machine.ms.FooServiceTest.shouldCompute","status":"FAILED","durationMs":120,"errorSignature":"assert"}]}
JSON
curl -fsS --max-time 10 -X POST http://127.0.0.1:18092/api/v1/ingestion/reportportal/test-runs \
  -H "Content-Type: application/json" --data-binary @/tmp/ms-test-run.json >/dev/null

cat >/tmp/ms-test-changes.json <<'JSON'
{"requestId":"req-full-chg","service":"ms-plan","branch":"main","baseCommit":"ab12cd34","headCommit":"ff89aa10","changes":[{"filePath":"src/main/java/com/machine/ms/Foo.java","methodSig":"compute()","lineStart":10,"lineEnd":40,"changeType":"MODIFIED"}]}
JSON
curl -fsS --max-time 10 -X POST http://127.0.0.1:18092/api/v1/ingestion/commit-changes \
  -H "Content-Type: application/json" --data-binary @/tmp/ms-test-changes.json >/dev/null

cat >/tmp/ms-test-art.json <<JSON
{"requestId":"req-full-art","runId":"$RUN_ID","artifacts":[{"artifactType":"REPORTPORTAL_LAUNCH","storage":"REPORTPORTAL","url":"$LAUNCH_LINK","checksum":"$LAUNCH_UUID"}]}
JSON
curl -fsS --max-time 10 -X POST http://127.0.0.1:18092/api/v1/ingestion/artifacts \
  -H "Content-Type: application/json" --data-binary @/tmp/ms-test-art.json >/dev/null

rp_api GET "/api/v1/default_personal/launch/$LAUNCH_UUID" | rg -q "\"status\":\"FAILED\""
curl -fsS --max-time 10 "http://127.0.0.1:18092/api/v1/runs/$RUN_ID/evidence" | rg -q "REPORTPORTAL_LAUNCH"
curl -fsS --max-time 10 "http://127.0.0.1:18092/api/v1/tests/com.machine.ms.FooServiceTest.shouldCompute/history?service=ms-plan&branch=main&limit=5" | rg -q "history"
curl -fsS --max-time 10 "http://127.0.0.1:18092/api/v1/impact/compute-from-commits" -X POST \
  -H "Content-Type: application/json" --data '{"service":"ms-plan","baseCommit":"ab12cd34","headCommit":"ff89aa10","branch":"main"}' \
  | rg -q "impactedTests"

TABLE_COUNT=$("${COMPOSE_CMD[@]}" exec -T postgres psql -U ms_test -d ms_test -Atc "select count(*) from ms_test_run_snapshots;")
[[ "$TABLE_COUNT" -ge 1 ]] || { echo "expected persisted runs in postgres" >&2; exit 1; }

if [[ "${MS_INFRA_E2E_ENABLED:-false}" == "true" ]]; then
  curl -fsS --max-time 5 "$MS_INFRA_HEALTH_URL" | rg -q "toolName|status"
  INFRA_RESPONSE="$(curl -sS --max-time 10 -X POST http://127.0.0.1:18092/api/v1/integration/ms-infra/test-run/start-and-poll \
    -H "Content-Type: application/json" \
    --data '{"workflowId":"quality.yml","inputs":{"selectionMode":"IMPACTED_ONLY","tests":["com.machine.ms.FooServiceTest.shouldCompute"]},"pollAttempts":1,"pollSleepMs":200}' \
  )"
  if [[ "$MS_INFRA_E2E_REQUIRE_SUCCESS" == "true" ]]; then
    echo "$INFRA_RESPONSE" | rg -q "runId"
  else
    echo "$INFRA_RESPONSE" | rg -q "runId|errorCode"
  fi
fi

echo "E2E full stack (ReportPortal + ms-test + PostgreSQL): OK"
