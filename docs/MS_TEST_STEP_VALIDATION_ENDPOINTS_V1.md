# ms-test Step Validation Endpoints (V1 implementation contract)

Ce document implemente la step 3 du plan 1959 au niveau contrat API:
- `POST /api/v1/steps/diff`
- `POST /api/v1/steps/impacted-tests`
- `POST /api/v1/steps/gates/evaluate`

## 1) Correlation keys obligatoires
Tous les endpoints exigent les champs:
- `stepId` (string non vide)
- `phase` (`EXECUTION|REVIEW`)
- `baseSha` (sha git)
- `headSha` (sha git)
- `requestId` (uuid ou id de correlation ms-mcp)

Validation commune:
- `baseSha != headSha`
- pattern SHA: `^[a-f0-9]{7,64}$`
- `requestId` doit etre propage dans les logs et dans la reponse.

## 2) POST /api/v1/steps/diff

### Request
```json
{
  "stepId": "step-1959-03",
  "phase": "EXECUTION",
  "baseSha": "ab12cd34",
  "headSha": "ff89aa10",
  "requestId": "req-2e6f04c0"
}
```

### Response 200
```json
{
  "requestId": "req-2e6f04c0",
  "stepId": "step-1959-03",
  "phase": "EXECUTION",
  "baseSha": "ab12cd34",
  "headSha": "ff89aa10",
  "diffId": "diff_87421",
  "filesChanged": 4,
  "changedFiles": [
    "src/main/java/FooService.java",
    "src/test/java/FooServiceTest.java"
  ],
  "generatedAt": "2026-03-02T16:19:00Z"
}
```

## 3) POST /api/v1/steps/impacted-tests

### Request
```json
{
  "stepId": "step-1959-03",
  "phase": "REVIEW",
  "baseSha": "ab12cd34",
  "headSha": "ff89aa10",
  "requestId": "req-95f42f72"
}
```

### Response 200
```json
{
  "requestId": "req-95f42f72",
  "stepId": "step-1959-03",
  "phase": "REVIEW",
  "baseSha": "ab12cd34",
  "headSha": "ff89aa10",
  "impactId": "impact_551",
  "tests": [
    {
      "testId": "com.machine.FooServiceTest#shouldCompute",
      "confidence": 0.93,
      "reasons": ["method_call_graph", "same_package"]
    }
  ],
  "generatedAt": "2026-03-02T16:19:03Z"
}
```

## 4) POST /api/v1/steps/gates/evaluate

### Request
```json
{
  "stepId": "step-1959-03",
  "phase": "REVIEW",
  "baseSha": "ab12cd34",
  "headSha": "ff89aa10",
  "requestId": "req-4f5904b9",
  "policyId": "validation-policy-v1",
  "runId": "run_789"
}
```

### Response 200 (PASS)
```json
{
  "requestId": "req-4f5904b9",
  "stepId": "step-1959-03",
  "phase": "REVIEW",
  "baseSha": "ab12cd34",
  "headSha": "ff89aa10",
  "policyId": "validation-policy-v1",
  "runId": "run_789",
  "gateReportId": "gate_9001",
  "status": "PASS",
  "reasons": [],
  "evaluatedAt": "2026-03-02T16:19:11Z",
  "idempotentReplay": false
}
```

### Response 200 (FAIL)
```json
{
  "requestId": "req-4f5904b9",
  "stepId": "step-1959-03",
  "phase": "REVIEW",
  "baseSha": "ab12cd34",
  "headSha": "ff89aa10",
  "policyId": "validation-policy-v1",
  "runId": "run_789",
  "gateReportId": "gate_9002",
  "status": "FAIL",
  "reasons": ["failed_tests_present"],
  "evaluatedAt": "2026-03-02T16:19:11Z",
  "idempotentReplay": false
}
```

## 5) Idempotence contract (gates/evaluate)
Cle d'idempotence metier:
`(stepId, phase, baseSha, headSha, policyId, runId)`.

Regle:
- meme tuple => reponse identique (meme `gateReportId`, meme `status`), avec `idempotentReplay=true`.
- tuple different => nouveau calcul + nouveau `gateReportId`.
- TTL d'index idempotence: 30 jours minimum.

## 6) Persistance audit (owner: ms-test)
`ms-test` persiste uniquement ses preuves analytiques:
- `diffId`, `impactId`, `gateReportId`
- correlation keys (`stepId`, `phase`, `baseSha`, `headSha`, `requestId`, `runId`, `policyId`)
- horodatage + version moteur

Anti-duplication:
- ne pas persister les logs infra complets (`ms-infra` owner)
- ne pas persister l'etat workflow/checklist (`ms-plan` owner)

## 7) Erreurs minimales
- `400 INVALID_INPUT` (sha invalide, phase invalide, champ manquant)
- `409 GATE_IDEMPOTENCY_CONFLICT` (meme id externe mais tuple metier incompatible)
- `422 UNSUPPORTED_STEP_PHASE` (phase non supportee)
- `503 UPSTREAM_UNAVAILABLE` (provider interne indisponible)

## 8) Done criteria step 3
- Les 3 endpoints existent avec schemas request/response stricts.
- Correlation keys obligatoires sur toutes les routes.
- Idempotence gate appliquee sur tuple metier complet.
- Audit diff/impact/gate persiste uniquement cote `ms-test`.
