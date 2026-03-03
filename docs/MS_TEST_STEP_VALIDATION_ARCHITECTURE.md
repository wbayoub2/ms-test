# ms-test - Architecture Step Validation (separation clean)

## 1) Decision cible (confirmee)
- `ms-mcp` orchestre les tools et les appels inter-microservices.
- `ms-plan` est la jonction plan/OpenProject et expose des tools plan.
- `ms-test` porte la logique test/coverage/impact/gate.
- `ms-infra` execute les runs de tests et fournit les artefacts.

`ms-plan` n'orchestre pas `ms-test` et `ms-infra`.

## 2) Responsabilites par microservice

## 2.1 `ms-mcp` (orchestrateur unique)
Responsable de:
- construire les workflows tools agent (execution/review/validation);
- appeler `ms-plan`, `ms-test`, `ms-infra` dans le bon ordre;
- exposer des tools composites cote agent.

Ne fait pas:
- calcul metier TIA;
- execution CI native.

## 2.2 `ms-plan` (plan + OpenProject)
Responsable de:
- exposer les donnees de step/phase;
- exposer la jonction OpenProject (work package, metadata step);
- exposer les operations checklist/validation de plan.

Ne fait pas:
- calcul diff technique;
- calcul impacted tests;
- lancement de tests.

## 2.3 `ms-test` (metier QA)
Responsable de:
- diff technique (fichiers/methodes/lignes);
- impacted tests;
- gate evaluation;
- traceabilite commit <-> test <-> code.

## 2.4 `ms-infra` (execution)
Responsable de:
- lancement runs;
- suivi status/progression;
- logs/artefacts (ReportPortal, MinIO).

## 3) Stockage des SHA par step

## 3.1 Source de verite step
La source de verite step est le domaine plan/OpenProject (via `ms-plan`).

Mode recommande:
- SHA stockes dans OpenProject custom fields (ou table `ms-plan` miroir synchronisee OpenProject).

Champs minimaux:
- `exec_base_sha`
- `exec_head_sha`
- `review_base_sha`
- `review_head_sha`
- `validated_sha`
- `validation_policy_id`
- `last_gate_report_id`

## 3.2 Donnees analytiques
`ms-test` conserve une copie analytique (audit/forensics), mais pas la verite workflow de step.

## 4) Checklist review (fichier par fichier)
La checklist appartient au domaine plan:
- geree par `ms-plan` (ou OpenProject via `ms-plan`);
- synchronisee depuis le diff courant fourni par `ms-test`.

Statuts item:
- `TODO|IN_REVIEW|DONE|BLOCKED|WAIVED`

Regle dynamique:
- si `review_head_sha` change, checklist devient `STALE`, puis sync obligatoire.

## 5) Flux execution/review valide
Orchestration par `ms-mcp`:
1. lire contexte step via `ms-plan` (SHA + phase + policy);
2. demander diff/impacted a `ms-test`;
3. lancer run cible via `ms-infra`;
4. poll run via `ms-infra`;
5. evaluer gate via `ms-test`;
6. lire statut checklist via `ms-plan`;
7. demander validation step via `ms-plan` si conditions remplies.

## 6) Reponse agent standard
Les tools exposes a l'agent retournent:
- `state: OK|BLOCKED|RUNNING`
- `message`
- `data`

Le detail technique complet reste dans les services internes/logs.

## 7) Invariants non negociables
- aucune validation step sans gate `PASS` sur `headSha` courant;
- aucune validation step avec checklist incomplete/stale;
- run obsoletes (SHA differents) rejetes;
- toute decision liee a `stepId`, `phase`, `baseSha`, `headSha`.
