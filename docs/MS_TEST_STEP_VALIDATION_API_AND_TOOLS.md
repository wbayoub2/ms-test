# ms-test - Contrat API/Tools (orchestration par ms-mcp)

## 1) Regle structurante
- Les tools agent sont exposes par `ms-mcp`.
- `ms-mcp` orchestre les appels vers `ms-plan`, `ms-test`, `ms-infra`.
- `ms-plan` n'orchestre pas les autres microservices.

## 2) Tools exposes a l'agent par `ms-mcp`

## 2.1 Execution
1. `plan.step_execution_diff_get`
2. `plan.step_execution_impacted_tests_get`
3. `plan.step_execution_impacted_tests_run`
4. `plan.step_execution_run_get`

## 2.2 Review
5. `plan.step_review_diff_get`
6. `plan.step_review_impacted_tests_get`
7. `plan.step_review_impacted_tests_run`
8. `plan.step_review_run_get`
9. `plan.step_review_checklist_get`
10. `plan.step_review_checklist_update`
11. `plan.step_review_checklist_sync`

## 2.3 Validation
12. `plan.step_validate_with_tests`

## 3) Reponse standard tool agent
```json
{
  "state": "OK",
  "message": "Impacted tests passed",
  "data": {}
}
```

Etats autorises:
- `OK`
- `BLOCKED`
- `RUNNING`

## 4) Semantique claire get vs run

## 4.1 `*_get`
- ne declenche rien;
- retourne un etat calcule/existant.
- jamais d'effet de bord.

## 4.2 `*_run`
- declenche un run cible;
- retourne immediatement `state=RUNNING` + `runId`.
- l'agent ne bloque pas sur l'execution complete.

## 4.3 `*_run_get`
- suit le run;
- retourne:
  - soit succes global;
  - soit liste courte des tests en erreur.
- peut retourner `RUNNING` tant que le run n'est pas termine.

## 5) Resultats run visibles par l'agent

## 5.1 Tout passe
```json
{
  "state": "OK",
  "message": "All impacted tests passed",
  "data": {
    "runId": "run_789",
    "status": "PASSED",
    "summary": {"total": 12, "passed": 12, "failed": 0, "skipped": 0},
    "canValidateStep": true
  }
}
```

## 5.2 Echecs
```json
{
  "state": "BLOCKED",
  "message": "Impacted tests failed",
  "data": {
    "runId": "run_789",
    "status": "FAILED",
    "summary": {"total": 12, "passed": 10, "failed": 2, "skipped": 0},
    "failedTests": [
      {"testId": "com.machine.FooServiceTest#shouldCompute", "shortError": "Expected 200 but was 500"}
    ],
    "canValidateStep": false
  }
}
```

Regle:
- si run sur SHA obsolete: `state=BLOCKED`, `message=Run outdated for current headSha`.

## 6) Orchestration interne par tool (ms-mcp)

## 6.1 `plan.step_execution_diff_get`
1. `ms-mcp -> ms-plan` (context step execution, SHA)
2. `ms-mcp -> ms-test` (`/steps/diff`)
3. retour agent

## 6.2 `plan.step_execution_impacted_tests_get`
1. `ms-mcp -> ms-plan` (context)
2. `ms-mcp -> ms-test` (`/steps/impacted-tests`)
3. retour agent

## 6.3 `plan.step_execution_impacted_tests_run`
1. context via `ms-plan`
2. impacted via `ms-test`
3. run start via `ms-infra`
4. retour `RUNNING + runId`

## 6.4 `plan.step_execution_run_get`
1. status run via `ms-infra`
2. mapping en `OK|BLOCKED|RUNNING`

## 6.5 `plan.step_review_diff_get`
Identique execution, avec `review_base_sha -> review_head_sha`.

## 6.6 `plan.step_review_impacted_tests_get`
Identique execution, en contexte review.

## 6.7 `plan.step_review_impacted_tests_run`
Identique execution, en contexte review.

## 6.8 `plan.step_review_run_get`
Identique `execution_run_get`.

## 6.9 `plan.step_review_checklist_sync`
1. context review via `ms-plan`
2. diff via `ms-test`
3. sync checklist via `ms-plan`

## 6.10 `plan.step_review_checklist_get/update`
- appels `ms-plan` uniquement.

## 6.11 `plan.step_validate_with_tests`
1. context step via `ms-plan`
2. checklist status via `ms-plan`
3. impacted via `ms-test`
4. run start/poll via `ms-infra`
5. gate evaluate via `ms-test`
6. validate step via `ms-plan` si conditions ok

## 7) APIs internes attendues

## 7.1 `ms-plan`
- `GET /api/v1/steps/{stepId}/context`
- `GET /api/v1/steps/{stepId}/review/checklist`
- `POST /api/v1/steps/{stepId}/review/checklist/sync`
- `PATCH /api/v1/steps/{stepId}/review/checklist/items/{itemId}`
- `POST /api/v1/steps/{stepId}/validate`

## 7.2 `ms-test`
- `POST /api/v1/steps/diff`
- `POST /api/v1/steps/impacted-tests`
- `POST /api/v1/steps/gates/evaluate`
- Contrat request/response + idempotence + audit: `MS_TEST_STEP_VALIDATION_ENDPOINTS_V1.md`

## 7.3 `ms-infra`
- `POST /api/v1/quality/test-run/start`
- `GET /api/v1/quality/test-run/{runId}`

## 8) Stockage des informations
- SHA step/checklist/etat validation: domaine plan (OpenProject via `ms-plan`).
- diff/impact/gate/audit: `ms-test`.
- runs/logs/artefacts: `ms-infra`.

## 8.1 Contrat "qui fait quoi" (sans ambiguite)
- `ms-mcp`:
  - expose les tools agent;
  - compose les appels vers les autres MS;
  - mappe les erreurs techniques en reason codes stables.
- `ms-plan`:
  - maintient le workflow step/checklist/validation;
  - ne calcule jamais impacted tests, diff technique, gate.
- `ms-test`:
  - calcule diff, impacted tests, gate;
  - ne valide jamais une step.
- `ms-infra`:
  - execute les tests demandes;
  - ne decide jamais si une step doit etre validee.

## 8.2 Erreurs exposees a l'agent
L'agent ne voit pas les erreurs internes brutes de chaque microservice.
Il voit uniquement des reason codes normalises par `ms-mcp`.

Codes minimaux:
- `RUN_IN_PROGRESS`
- `RUN_OUTDATED_SHA`
- `IMPACTED_TESTS_FAILED`
- `CHECKLIST_INCOMPLETE`
- `CHECKLIST_STALE`
- `GATE_FAILED`
- `UPSTREAM_UNAVAILABLE`

Mapping attendu:
- timeout/reseau upstream -> `UPSTREAM_UNAVAILABLE`
- run termine avec failed tests -> `IMPACTED_TESTS_FAILED`
- run calcule sur SHA ancien -> `RUN_OUTDATED_SHA`
- checklist non complete -> `CHECKLIST_INCOMPLETE`
- checklist invalidee par nouveau diff -> `CHECKLIST_STALE`
- gate metier KO -> `GATE_FAILED`

## 9) Definition of done
1. `ms-mcp` orchestre tous les tools agent.
2. `ms-plan` ne fait pas d'orchestration inter-services.
3. Diff/impacted/run/checklist/validation utilisables en execution et review.
4. L'agent voit soit succes global, soit erreurs courtes des tests rates.
5. Validation impossible sans checklist OK + gate PASS + run sur SHA courant.

## 10) Exigences de qualite des tools `ms-mcp`
Pour chaque tool expose:
- description metier en 1 phrase (quand l'utiliser)
- preconditions explicites (phase, SHA necessaires)
- schema input strict
- schema output strict
- exemples request/response
- reason codes possibles
- idempotence/side effects documentes

Reason codes minimum:
- `RUN_IN_PROGRESS`
- `RUN_OUTDATED_SHA`
- `IMPACTED_TESTS_FAILED`
- `CHECKLIST_INCOMPLETE`
- `CHECKLIST_STALE`
- `GATE_FAILED`
- `UPSTREAM_UNAVAILABLE`
