# Multi-MS - Implementation Plan (Step Validation E2E)

## 1) Scope
Objectif: implementer une validation de step robuste et industrielle avec separation clean:
- `ms-mcp` orchestre les tools et appelle les autres microservices.
- `ms-plan` expose uniquement le domaine plan/OpenProject (step context, checklist, validation).
- `ms-test` calcule diff/impacted-tests/gate.
- `ms-infra` execute les runs de tests et expose status/logs/artefacts.

## 2) Contrat de responsabilites (non negociable)

## 2.1 `ms-mcp`
- expose les tools agent `plan.*`.
- orchestre les appels inter-services.
- assemble la reponse agent finale.

Interdit dans `ms-mcp`:
- logique metier TIA/gate;
- stockage metier durable de step.

## 2.2 `ms-plan`
- source de verite workflow step/phase.
- jonction OpenProject (custom fields + etat step).
- checklist review fichier par fichier.
- operation de validation step.

Interdit dans `ms-plan`:
- calcul diff technique;
- calcul tests impactes;
- execution tests.

## 2.3 `ms-test`
- diff technique `baseSha -> headSha`.
- impacted-tests + confidence/reasons.
- evaluation gate.
- audit analytique test-wise.

## 2.4 `ms-infra`
- run start/poll/summary.
- logs et artefacts (ReportPortal/MinIO).

## 3) Data ownership et stockage

## 3.1 Donnees plan (via `ms-plan` + OpenProject)
- `exec_base_sha`
- `exec_head_sha`
- `review_base_sha`
- `review_head_sha`
- `validated_sha`
- `validation_policy_id`
- `last_run_id`
- `last_gate_report_id`
- `checklist_version`
- `checklist_is_stale`

## 3.2 Donnees analytiques (`ms-test`)
- diff snapshots
- impacted-tests snapshots
- gate reports
- traceabilite commit <-> test <-> code

## 3.3 Donnees execution (`ms-infra`)
- run lifecycle
- test summary
- failed tests (short)
- logs et links artefacts

Regle anti-duplication:
- duplication autorisee uniquement pour IDs de correlation (`stepId`, `runId`, `gateReportId`, `headSha`).
- pas de recopie complete des logs/coverage en dehors du service proprietaire.

## 4) Tools `ms-mcp` a exposer

## 4.1 Execution phase
1. `plan.step_execution_diff_get`
2. `plan.step_execution_impacted_tests_get`
3. `plan.step_execution_impacted_tests_run`
4. `plan.step_execution_run_get`

## 4.2 Review phase
5. `plan.step_review_diff_get`
6. `plan.step_review_impacted_tests_get`
7. `plan.step_review_impacted_tests_run`
8. `plan.step_review_run_get`
9. `plan.step_review_checklist_get`
10. `plan.step_review_checklist_update`
11. `plan.step_review_checklist_sync`

## 4.3 Validation phase
12. `plan.step_validate_with_tests`

## 5) Reponse agent standard
Tous les tools `plan.*` retournent:
- `state`: `OK|BLOCKED|RUNNING`
- `message`: texte court
- `data`: donnees utiles

Run outcome visible agent:
- succes: resume global (`total/passed/failed/skipped`)
- echec: resume + `failedTests[]` avec `testId` + `shortError`

## 6) Flux de reference (orchestration ms-mcp)

## 6.1 Execution
1. `ms-mcp -> ms-plan` get context execution
2. `ms-mcp -> ms-test` diff/impacted
3. `ms-mcp -> ms-infra` run start
4. `ms-mcp -> ms-infra` run poll

## 6.2 Review
1. `ms-mcp -> ms-plan` get context review
2. `ms-mcp -> ms-test` diff/impacted
3. `ms-mcp -> ms-plan` checklist sync/get/update
4. `ms-mcp -> ms-infra` run start/poll

Regle dynamique:
- si `review_head_sha` change, checklist stale + impacted recalcul obligatoire.

## 6.3 Validation
1. lire context + checklist status
2. verifier run valide sur SHA courant
3. evaluer gate (`ms-test`)
4. si conditions ok -> `ms-plan` validate step

Conditions strictes:
- checklist complete (`DONE` ou `WAIVED` justifie)
- run `PASSED`
- run sur `headSha` courant
- gate `PASS`

## 7) Implementation step-by-step (plan detaille)

### Step 1 - Contrat architecture
Livrables:
- ADR responsabilites + anti-responsabilites
- tableau d'appels autorises (`ms-mcp` -> autres)

Verification:
- aucune regle TIA/gate dans `ms-mcp` et `ms-plan`.

### Step 2 - Modele de donnees `ms-plan`/OpenProject
Livrables:
- schema/champs custom fields
- checklist entities + stale/version
- checklist par defaut non vide a la creation d'une step

Verification:
- un step possede SHA execution/review/validation cohérents.
- impossible de valider une step si la checklist est vide ou incomplete.

### Step 3 - API `ms-test`
Livrables:
- `POST /api/v1/steps/diff`
- `POST /api/v1/steps/impacted-tests`
- `POST /api/v1/steps/gates/evaluate`

Verification:
- idempotence gate sur meme tuple (`stepId`, `phase`, `baseSha`, `headSha`, `policyId`).

### Step 4 - API `ms-infra`
Livrables:
- start run cible
- get run status
- failed tests courts

Verification:
- run sur liste de tests explicite + metadata step/phase/SHA.

### Step 5 - Tools `ms-mcp`
Livrables:
- 12 tools `plan.*` exposes
- orchestration complete par phase

Verification:
- chaque tool appelle les bons services dans le bon ordre.

### Step 6 - Normalisation sortie agent
Livrables:
- mapper de sortie `OK|BLOCKED|RUNNING`
- reason codes stables

Verification:
- pas d'erreur technique brute dans output agent.

### Step 7 - Checklist review dynamique
Livrables:
- sync checklist depuis diff
- update item status/note

Verification:
- checklist stale si `review_head_sha` change.

### Step 8 - Data ownership/no duplication
Livrables:
- matrice ownership finale
- restrictions duplication appliquees

Verification:
- seules correlations minimales sont dupliquees.

### Step 9 - E2E tests
Cas obligatoires:
1. execution PASS
2. review avec modif reviewer -> recalcul impacted
3. run FAIL -> BLOCKED + failedTests courts
4. checklist incomplete -> validation refusee
5. checklist complete + gate PASS + run SHA courant -> validation acceptee

### Step 10 - Rollout
Livrables:
- activation progressive (advisory puis blocking)
- dashboard SLO
- runbook incident

Verification:
- rollback pilotable via feature flags.

## 8) Detail obligatoire par microservice (a implementer)

## 8.1 `ms-test` - ce qu'il faut creer
Obligatoire V1:
- ingestion OpenClover (test-wise coverage + mapping lignes/methodes)
- ingestion ReportPortal (statuts tests/run metadata)
- index commit/diff (baseSha/headSha) lie au `stepId`
- endpoints:
  - `POST /api/v1/steps/diff`
  - `POST /api/v1/steps/impacted-tests`
  - `POST /api/v1/steps/gates/evaluate`
- persistence:
  - snapshots diff
  - snapshots impacted-tests
  - rapports gate (idempotents)
  - traceability commit <-> test <-> code

Attendu:
- aucune logique workflow step dans `ms-test`
- API stable, versionnee, avec correlation keys obligatoires
- gate deterministic pour meme tuple d'entree

## 8.2 `ms-plan` - ce qu'il faut creer/adapter
Obligatoire V1:
- contexte step expose (`execution`/`review`) avec SHA
- checklist review fichier par fichier
- operation de validation step
- integration OpenProject (custom fields ou projection interne)

Attendu:
- pas de calcul diff/TIA/gate
- stockage fiable de la verite workflow

## 8.3 `ms-infra` - ce qu'il faut creer/adapter
Obligatoire V1:
- lancement run sur selection explicite de tests impactes
- status poll + resume final + failed tests courts
- liens logs/artefacts

Attendu:
- execution technique uniquement
- pas de decision metier de gate/validation

## 8.4 `ms-mcp` - ce qu'il faut creer
Obligatoire V1:
- tools composites `plan.*` (12 tools)
- orchestration multi-services
- mapping de sortie agent `OK|BLOCKED|RUNNING`
- reason codes stables

Attendu:
- tool metadata complete pour chaque tool:
  - description metier claire
  - input schema complet
  - output schema complet
  - exemples request/response
  - erreurs mappees vers reason codes

## 9) Tests a creer (detail par couche)

## 9.1 Tests `ms-test`
- unitaires:
  - calcul diff (fichiers/methodes/lignes)
  - impacted-tests ranking/confidence
  - gate policy evaluation
- integration:
  - ingestion Clover/ReportPortal
  - idempotence gate report
  - contrat JSON endpoints

## 9.2 Tests `ms-plan`
- unitaires:
  - coherence transitions step/phase
  - checklist stale/version logic
- integration:
  - OpenProject field mapping
  - validate refusee si checklist incomplete

## 9.3 Tests `ms-infra`
- unitaires:
  - mapping payload run start
  - summary extraction
- integration:
  - run lifecycle start/poll/end
  - failedTests short extraction

## 9.4 Tests `ms-mcp`
- contract tests tools:
  - schema input/output par tool
  - mapping `OK|BLOCKED|RUNNING`
- orchestration tests:
  - ordre d'appel inter-services
  - retry/timeouts
  - outdated SHA handling

## 9.5 E2E obligatoires
1. execution PASS
2. review + commit reviewer -> stale + resync + recalcul impacted
3. run FAIL -> BLOCKED + failedTests courts
4. checklist incomplete -> validation refusee
5. run outdated SHA -> BLOCKED
6. checklist complete + run PASS + gate PASS -> validation acceptee

## 10) Limites et contraintes explicites
- latence cible de calcul impacted: < 5s sur diff standard
- timeout poll run configurable
- max tests lances par run configurable
- payload tools versionnes (breaking changes interdites sans v2)
- audit trail obligatoire (requestId/stepId/baseSha/headSha/runId/gateReportId)
- policy gate versionnee et tracable

## 11) Documentation attendue (Definition of Ready/Done)
Definition of Ready (avant dev):
- contrat responsabilites signe
- schemas API validés
- tool catalog ms-mcp approuve
- strategy test validee

Definition of Done (par step):
- code + tests + docs mis a jour
- contract tests passes
- evidence d'integration jointe
- aucun contournement du contrat de responsabilite

## 12) Gates d'acceptation E2E (DoD globale)
1. `ms-mcp` orchestre tous les tools cross-MS.
2. `ms-plan` reste domaine plan/OpenProject uniquement.
3. `ms-test` porte toute la logique diff/impact/gate.
4. `ms-infra` porte execution/run/logs/artefacts.
5. data ownership respecte, sans duplication inutile.
6. validation step fiable en E2E sur SHA courant.

## 13) Reference plan MCP
Plan cree dans `multi-ms` via tools ms-plan:
- `repositoryId`: `17`
- `planId`: `1959`
- `name`: `Architecture Step Validation E2E (ms-mcp orchestration, ms-plan bridge, ms-test QA, ms-infra execution)`
