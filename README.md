# ms-test

## Positioning

`ms-test` ne porte pas la topologie E2E. Le runtime, le control plane, le fingerprint et ReportPortal des runs E2E infra sont portes par `ms-infra`.

`ms-test` conserve:
- la consultation QA;
- le diff et les impacted tests;
- le gate;
- l'evidence logique liee a un `runId`.

## Validation recommandee

Validation locale `ms-test`:
```bash
cd /home/machine/Documents/Machine_MS/ms-test
mvn -q -Dtest=MsInfraIntegrationApiTest,MsInfraE2eConsultationApiTest,MsInfraE2eRunLinkingTest,MsTestPlanToInfraToTestChainApiTest test
```

Orchestration E2E infra nominale:
```bash
cd /home/machine/Documents/Machine_MS/ms-infra
E2E_ENV_FILE=/home/machine/Documents/Machine_MS/ms-infra/.env scripts/e2e/ci/run_e2e_target.sh --mode mock --target doc-chain
E2E_ENV_FILE=/home/machine/Documents/Machine_MS/ms-infra/.env scripts/e2e/ci/run_e2e_target.sh --mode mock --target mcp-chain
```

Les wrappers legacy `reportportal_stack.sh`, `e2e_full_stack_reportportal.sh` et `e2e_real_stack.sh` ont ete retires apres alignement fonctionnel prouve avec le control plane `ms-infra`.

## ms-infra integration notes

`ms-test` appelle `ms-infra` comme backend E2E via:
- `POST /api/v1/integration/ms-infra/test-run/start-and-poll` cote `ms-test`
- `POST /api/v1/e2e/runs` cote `ms-infra`
- `GET /api/v1/e2e/runs/{runId}` cote `ms-infra`
- `GET /api/v1/e2e/runs/{runId}/artifacts` cote `ms-infra`

Vue QA agregée exposee par `ms-test`:
- `GET /api/v1/e2e/runs/{runId}`
- reponse: `runId`, `status`, `gateStatus`, `impactedTests`, `evidence`, `reportPortalLaunchId`, `reportPortalUrl`

When running `ms-test` in docker profile, configure:
- `MS_TEST_MS_INFRA_ENABLED=true`
- `MS_TEST_MS_INFRA_BASE_URL=http://host.docker.internal:18081`
- `MS_TEST_MS_INFRA_COOKIE=<session cookie if required>`
- `MS_TEST_MS_INFRA_AUTHORIZATION=Bearer <token if required>`
- `MS_TEST_MS_INFRA_SESSION_ID=<session-id for traceability>`

This is already supported in:
- [application-docker.yml](/home/machine/Documents/Machine_MS/ms-test/src/main/resources/application-docker.yml)
- [MsInfraHttpToolClient.java](/home/machine/Documents/Machine_MS/ms-test/src/main/java/com/machine/ms/test/infra/integration/MsInfraHttpToolClient.java)
