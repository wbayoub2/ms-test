# ms-test

## Full stack local (ReportPortal + ms-test + PostgreSQL)

### 1) Start/stop ReportPortal full stack
```bash
cd /home/machine/Documents/Machine_MS/ms-test
./scripts/reportportal_stack.sh start
./scripts/reportportal_stack.sh status
./scripts/reportportal_stack.sh token
./scripts/reportportal_stack.sh stop
```

Defaults used by the script:
- user: `default`
- password: `1q2w3e`
- OAuth client: `ui:uiman`
- URL: `http://127.0.0.1:28080`

Override them with:
- `MS_TEST_RP_PROJECT`
- `MS_TEST_RP_RUNTIME_DIR`
- `MS_TEST_RP_BASE_URL`
- `MS_TEST_RP_PORT`
- `MS_TEST_RP_TRAEFIK_PORT`
- `MS_TEST_RP_HTTPS_PORT`
- `MS_TEST_RP_ADMIN_USER`
- `MS_TEST_RP_ADMIN_PASSWORD`
- `MS_TEST_RP_CLIENT_ID`
- `MS_TEST_RP_CLIENT_SECRET`

### 2) Run complete E2E (real RP launch + ms-test ingestion + DB checks)
```bash
cd /home/machine/Documents/Machine_MS/ms-test
./scripts/e2e_full_stack_reportportal.sh
```

What this E2E validates:
1. ReportPortal is up and token retrieval works.
2. A real launch is created in ReportPortal API and finished with `FAILED`.
3. OpenClover/ReportPortal/commit/artifact payloads are ingested into `ms-test`.
4. Functional endpoints (`history`, `impact`, `evidence`) respond correctly.
5. Data is persisted in PostgreSQL (`ms_test_run_snapshots`).
6. Optional `ms-infra` bridge check when `MS_INFRA_E2E_ENABLED=true`.

### 3) Existing real-stack test without ReportPortal API creation
```bash
cd /home/machine/Documents/Machine_MS/ms-test
./scripts/e2e_real_stack.sh
```

## ms-infra integration notes

`ms-test` calls `ms-infra` bridge endpoint:
- `POST /api/v1/integration/ms-infra/test-run/start-and-poll`

When running `ms-test` in docker profile, configure:
- `MS_TEST_MS_INFRA_ENABLED=true`
- `MS_TEST_MS_INFRA_BASE_URL=http://host.docker.internal:18081`
- `MS_TEST_MS_INFRA_COOKIE=<session cookie if required>`
- `MS_TEST_MS_INFRA_AUTHORIZATION=Bearer <token if required>`
- `MS_TEST_MS_INFRA_SESSION_ID=<session-id for traceability>`

This is already supported in:
- [application-docker.yml](/home/machine/Documents/Machine_MS/ms-test/src/main/resources/application-docker.yml)
- [MsInfraHttpToolClient.java](/home/machine/Documents/Machine_MS/ms-test/src/main/java/com/machine/ms/test/infra/integration/MsInfraHttpToolClient.java)

To validate bridge from E2E script:
```bash
MS_INFRA_E2E_ENABLED=true MS_INFRA_BASE_URL=http://127.0.0.1:18081 ./scripts/e2e_full_stack_reportportal.sh
```

Notes:
- `MS_INFRA_E2E_REQUIRE_SUCCESS=true` enforces a real `runId` from ms-infra.
- Without valid GitHub provider credentials/workflow, local ms-infra may return a structured provider error; this still validates bridge connectivity and error contract when strict mode is off.
