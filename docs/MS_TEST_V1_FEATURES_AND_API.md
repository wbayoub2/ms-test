# ms-test - Fonctionnalites V1 et Contrat API

## 1) Objectif
`ms-test` est le service de traçabilité test-wise pour répondre aux besoins suivants:
- savoir quand un test passait/échouait, sur quel commit;
- savoir quel code exact (fichiers/methodes/lignes) était couvert par ce test à ce commit;
- sélectionner les tests réellement impactés par les changements de code;
- prioriser intelligemment les zones non couvertes.

Le principe est de réutiliser au maximum des outils existants:
- OpenClover: coverage test-wise + TIA;
- ReportPortal: statut/execution des tests + metadata;
- MinIO: archivage de preuves;
- PostgreSQL: index analytique;
- API exposée par `ms-test` (ou PostgREST en phase 1 pour minimiser le code Java).

## 2) Fonctionnalites V1

### 2.1 Traceabilite Test <-> Commit
1. `test_history_timeline`
- Historique par test: `PASS/FAIL/SKIP`, `commitSha`, `branch`, `timestamp`, `runId`, `service`.

2. `last_green_first_red`
- Calcul automatique du dernier commit vert et du premier commit rouge par test.

3. `test_status_at_commit`
- Reponse directe a: "ce test passait-il au commit X ?".

### 2.2 Coverage Test-wise
4. `testwise_coverage_snapshot`
- Stocke `test -> fichier -> methode -> lignes` pour chaque commit.

5. `coverage_diff_by_test`
- Compare commit A vs B pour un test (delta de lignes/methodes couvertes).

### 2.3 Impact Analysis (TIA)
6. `tia_impacted_tests`
- Entree: diff Git (fichiers/methodes/lignes modifies).
- Sortie: liste de tests impactes, tries.

7. `minimal_safe_test_set`
- Renvoie le set minimal de tests à lancer pour valider un commit/PR.

8. `tia_confidence_score`
- Score de confiance `HIGH/MEDIUM/LOW` par test impacte (direct/indirect).

### 2.4 Regression Forensics
9. `regression_forensics_report`
- Pour un test en echec: dernier vert, premier rouge, changements suspects.

10. `suspect_changes_rank`
- Classement des fichiers/methodes les plus probables dans la regression.

### 2.5 Coverage Intelligence
11. `uncovered_code_inventory`
- Inventaire des zones non couvertes (fichier/methode/ligne) par service.

12. `coverage_hotspots`
- Priorisation combinee: non-couvert + churn Git + complexite.

13. `qa_prioritized_backlog`
- Backlog QA prêt à agir (top cibles de tests à ajouter).

### 2.6 Evidence et Audit
14. `artifact_evidence_registry`
- Traçage des preuves (Clover HTML/XML, exports JSON, logs), stockees sur MinIO.

15. `launch_to_evidence_linking`
- Ajout automatique du lien MinIO dans le launch ReportPortal associe.

16. `audit_decision_log`
- Journal des decisions TIA (pourquoi ce test a ete selectionne).

### 2.7 Quick Wins (faible effort, forte valeur)
17. `flaky_test_signal`
- Detection des tests instables via historique PASS/FAIL (fenetre glissante de commits).

18. `cross_microservice_traceability`
- Vue agregee multi-services pour un meme test logique ou une meme classe.

19. `quality_gate_enriched`
- Gate CI enrichie: echec si regression sur tests critiques impactes ou evidence manquante.

20. `run_catalog_search`
- Recherche rapide des runs par `service/branch/commit/status`.

21. `test_catalog_search`
- Recherche rapide des tests par nom, package, statut recent, criticite.

22. `commit_test_matrix`
- Matrice commit -> tests executes -> statuts -> liens evidence.

## 3) Mise en place propre (faible code)

## 3.1 Prerequis outils
- OpenClover integre dans Maven de chaque microservice Java.
- ReportPortal agent integre dans Surefire/Failsafe.
- MinIO bucket dedie, ex: `qa-evidence`.
- PostgreSQL dedie `ms_test`.

## 3.2 Contrat CI standard (par microservice)
A chaque run CI:
1. Recuperer metadata Git (`service`, `branch`, `commitSha`, `prNumber`).
2. Executer TIA OpenClover (`clover:optimize`) pour obtenir les tests cibles.
3. Executer tests + publication live vers ReportPortal avec attributs:
- `service:<name>`
- `commit:<sha>`
- `branch:<branch>`
4. Generer rapport Clover (HTML/XML).
5. Uploader artefacts sur MinIO:
- `s3://qa-evidence/<service>/<branch>/<commitSha>/<runId>/...`
6. Ingestion en base PostgreSQL (scripts):
- resultats tests depuis ReportPortal API;
- coverage test-wise depuis Clover XML/DB export;
- diff Git pour indexer changements.

## 3.3 Schema SQL minimal
Tables recommandees:
- `test_run(run_id, service, branch, commit_sha, started_at, ended_at, rp_launch_id, status)`
- `test_result(run_id, test_id, test_name, status, duration_ms, error_signature)`
- `test_coverage_line(run_id, test_id, file_path, class_name, method_sig, line_no, covered)`
- `commit_change(commit_sha, file_path, method_sig, line_start, line_end, change_type)`
- `artifact_link(run_id, artifact_type, storage, url, checksum)`
- `tia_decision(run_id, test_id, reason, confidence)`

Vues utiles:
- `v_last_green_first_red`
- `v_impacted_tests_by_commit`
- `v_uncovered_hotspots`

## 4) Contrat API V1
Base URL proposee:
- `/api/v1`

Principes de design:
- REST orientee ressources, sans verbes metier dans les paths sauf operations de calcul (`/impact/compute`).
- Reponses deterministes et paginees pour les listes.
- Identifiants stables pour le multi-microservice.
- Corrélation explicite avec la version exacte du code (`commitSha` + `treeSha` + `repository` + `branch`).

Convention d'identifiants:
- `service`: nom logique du microservice (`ms-plan`, `ms-memory`, ...).
- `testId`: identifiant canonique `fully.qualified.ClassName#method`.
- `runId`: identifiant unique d'execution CI.
- `commitSha`: SHA Git 40 caracteres.
- `treeSha`: SHA Git de l'arbre pour figer l'etat exact du code (optionnel mais recommande).

Format standard de reponse:
```json
{
  "requestId": "req_123",
  "data": {},
  "meta": {
    "service": "ms-plan",
    "repository": "github.com/org/ms-plan",
    "branch": "main",
    "commitSha": "abc123",
    "treeSha": "def456",
    "generatedAt": "2026-03-02T10:30:00Z"
  },
  "error": null
}
```

Format standard d'erreur:
```json
{
  "requestId": "req_123",
  "data": null,
  "meta": {},
  "error": {
    "code": "TEST_NOT_FOUND",
    "message": "No test found for id ...",
    "details": {}
  }
}
```

Codes d'erreur minimaux:
- `TEST_NOT_FOUND`
- `RUN_NOT_FOUND`
- `COMMIT_NOT_FOUND`
- `INVALID_INPUT`
- `IMPACT_COMPUTE_FAILED`
- `EVIDENCE_NOT_FOUND`

Pagination (recommandee pour toutes les listes):
- `limit` (defaut 100, max 1000)
- `cursor` (opaque)
- Reponse: `meta.pagination.nextCursor`

### 4.1 Traceabilite
1. `GET /tests/{testId}/history?service=ms-plan&branch=main&limit=100`
- Retourne timeline du test.

2. `GET /tests/{testId}/status-at/{commitSha}?service=ms-plan`
- Retourne statut exact du test au commit.

3. `GET /tests/{testId}/last-green-first-red?service=ms-plan&branch=main`
- Retourne `lastGreenCommit`, `firstRedCommit`.

4. `GET /tests/{testId}/compare?service=ms-plan&from=shaA&to=shaB`
- Retourne comparaison directe A vs B:
  - statut (`PASS/FAIL/SKIP`) a chaque commit,
  - delta coverage (fichiers/methodes/lignes),
  - resume des changements suspects.

### 4.2 Coverage test-wise
5. `GET /tests/{testId}/coverage-at/{commitSha}?service=ms-plan`
- Retourne fichiers/methodes/lignes couvertes.

6. `GET /tests/{testId}/coverage-diff?service=ms-plan&from=shaA&to=shaB`
- Retourne delta coverage du test.

### 4.3 Impact Analysis
7. `POST /impact/compute`
- Body:
```json
{
  "service": "ms-plan",
  "branch": "feature/my-change",
  "baseCommit": "shaA",
  "headCommit": "shaB",
  "changes": [
    {"filePath": "src/main/java/x/Foo.java", "lineStart": 10, "lineEnd": 45}
  ]
}
```
- Retour:
```json
{
  "impactedTests": [
    {"testId": "t1", "testName": "FooTest#shouldX", "confidence": "HIGH", "reasons": ["DIRECT_LINE_COVERAGE"]}
  ],
  "minimalSafeSet": ["t1", "t2"]
}
```

8. `POST /impact/compute-from-commits`
- Variante recommandee pour limiter les erreurs client:
```json
{
  "service": "ms-plan",
  "baseCommit": "shaA",
  "headCommit": "shaB",
  "branch": "feature/my-change"
}
```
- Le service calcule lui-meme le diff Git et renvoie:
  - `changedFiles`,
  - `changedMethods`,
  - `impactedTests`,
  - `minimalSafeSet`.

### 4.4 Regression Forensics
9. `GET /tests/{testId}/regression-forensics?service=ms-plan&branch=main`
- Retourne dernier vert, premier rouge, suspects classes/methodes.

10. `GET /commits/{commitSha}/suspects?service=ms-plan&testId=...`
- Retourne les changements les plus suspects.

### 4.5 Coverage Intelligence
11. `GET /coverage/uncovered?service=ms-plan&branch=main&limit=200`
- Inventaire non couvert.

12. `GET /coverage/hotspots?service=ms-plan&branch=main&limit=50`
- Liste priorisee des hotspots non couverts.

13. `GET /qa/backlog?service=ms-plan&branch=main`
- Backlog QA priorise (tests à creer en premier).

### 4.6 Evidence
14. `GET /runs/{runId}/evidence`
- Retourne les URLs MinIO des artefacts et checksums.

15. `GET /tests/{testId}/evidence-at/{commitSha}?service=ms-plan`
- Retourne les preuves de ce test pour un commit:
  - statut run,
  - liens Clover HTML/XML,
  - liens logs ReportPortal,
  - checksum artefacts.

16. `GET /runs/search?service=ms-plan&branch=main&status=FAILED&limit=100`
- Recherche de runs (quick win pour ops/QA).

17. `GET /tests/search?service=ms-plan&q=FooTest&status=FAILED&limit=100`
- Recherche de tests (quick win pour diagnostic rapide).

18. `GET /commits/{commitSha}/matrix?service=ms-plan`
- Matrice compacte des tests executes sur un commit:
  - `testId`, `status`, `durationMs`, `runId`, `evidenceUrl`.

19. `GET /tests/flaky?service=ms-plan&branch=main&window=50&minTransitions=3`
- Liste des tests potentiellement flaky (alternances PASS/FAIL).

20. `GET /services/overview?branch=main`
- Vue multi-microservice:
  - nombre de tests passes/fails,
  - couverture moyenne,
  - top regressions recentes.

21. `POST /quality-gates/evaluate`
- Evalue un gate de qualite enrichi sur un commit/PR:
```json
{
  "service": "ms-plan",
  "branch": "feature/my-change",
  "baseCommit": "shaA",
  "headCommit": "shaB",
  "policy": {
    "failOnCriticalTestRegression": true,
    "failOnMissingEvidence": true,
    "maxNewUncoveredHotspots": 3
  }
}
```
- Retour:
```json
{
  "decision": "FAIL",
  "reasons": [
    "CRITICAL_TEST_REGRESSION",
    "MISSING_EVIDENCE"
  ],
  "details": {
    "regressedTests": ["com.machine.FooTest#shouldX"],
    "missingEvidenceRuns": ["run_123"]
  }
}
```

22. `GET /quality-gates/policies`
- Retourne les policies de gate disponibles/versionnees.

### 4.7 Exemple de payload (compare A vs B)
`GET /tests/com.machine.FooTest%23shouldX/compare?service=ms-plan&from=shaA&to=shaB`
```json
{
  "requestId": "req_987",
  "data": {
    "testId": "com.machine.FooTest#shouldX",
    "from": {
      "commitSha": "shaA",
      "status": "PASS",
      "coveredLines": 42
    },
    "to": {
      "commitSha": "shaB",
      "status": "FAIL",
      "coveredLines": 31
    },
    "coverageDelta": {
      "added": [{"filePath": "src/main/java/x/Foo.java", "lineNo": 88}],
      "removed": [{"filePath": "src/main/java/x/Foo.java", "lineNo": 34}]
    },
    "suspects": [
      {"filePath": "src/main/java/x/Foo.java", "methodSig": "compute(int)", "score": 0.92}
    ]
  },
  "meta": {
    "service": "ms-plan",
    "repository": "github.com/org/ms-plan",
    "branch": "main",
    "generatedAt": "2026-03-02T10:30:00Z"
  },
  "error": null
}
```

## 5) Convention de metadata (obligatoire)
Attributs minimaux à propager partout:
- `service`
- `repository`
- `commitSha`
- `treeSha`
- `branch`
- `runId`
- `testId` / `testName`

Attribut ReportPortal recommande:
- `service:<service>`
- `commit:<commitSha>`
- `branch:<branch>`

## 6) Niveau de code attendu
- Phase 1 (minimum code):
  - SQL + vues + scripts d'ingestion (bash/python léger),
  - API via PostgREST.
- Phase 2:
  - Service Java `ms-test` pour règles avancées (`forensics`, scoring plus fin).

## 7) Critères d'acceptation V1
1. Pour un test donne, obtenir en < 5s son `lastGreen` et `firstRed`.
2. Pour un diff de commit, obtenir une liste de tests impactes et un set minimal.
3. Pour un commit, retrouver les preuves coverage/test dans MinIO.
4. Pour un service, sortir un top hotspots non couverts exploitable par QA.
5. Detecter automatiquement les tests flaky sur une fenetre de 50 commits.
6. Exposer une matrice commit->tests->status->evidence en < 5s.
7. Evaluer un quality gate enrichi en mode API pour blocage CI.

## 8) Verification explicite vs besoins primaires
1. Besoin A - \"Quand ce test passait exactement ?\"
- Endpoints: `history`, `status-at`, `last-green-first-red`.
- Donnees garanties: `commitSha`, `branch`, `timestamp`, `runId`, `repository`, `treeSha`.

2. Besoin B - \"Quel code exact etait execute par ce test a ce moment ?\"
- Endpoints: `coverage-at`, `coverage-diff`, `compare`.
- Donnees garanties: `filePath`, `className`, `methodSig`, `lineNo`, delta A/B.

3. Besoin TIA - \"Quels tests lancer maintenant ?\"
- Endpoints: `impact/compute` et `impact/compute-from-commits`.
- Donnees garanties: `impactedTests`, `minimalSafeSet`, `confidence`, `reasons`.

4. Besoin forensics regression
- Endpoints: `regression-forensics`, `commits/{sha}/suspects`.
- Donnees garanties: `lastGreen`, `firstRed`, `suspect rank`.

5. Besoin coverage intelligent QA
- Endpoints: `coverage/uncovered`, `coverage/hotspots`, `qa/backlog`.
- Donnees garanties: priorisation actionnable, pas seulement un pourcentage global.

6. Besoin operationnel quotidien (recherche rapide et fiabilite CI)
- Endpoints: `runs/search`, `tests/search`, `tests/flaky`, `quality-gates/evaluate`, `commits/{sha}/matrix`, `services/overview`.
- Donnees garanties: diagnostic rapide, anti-bruit flaky, gate CI deterministe.

## 9) Extensions Step Validation (execution + review)
Pour la validation par step avec diff courant, tests impactes, et gate avant validation:
- Voir [MS_TEST_STEP_VALIDATION_ARCHITECTURE.md](/home/machine/Documents/Machine_MS/ms-infra/ms-test/MS_TEST_STEP_VALIDATION_ARCHITECTURE.md)
- Voir [MS_TEST_STEP_VALIDATION_API_AND_TOOLS.md](/home/machine/Documents/Machine_MS/ms-infra/ms-test/MS_TEST_STEP_VALIDATION_API_AND_TOOLS.md)

Ces documents fixent:
- la separation stricte `ms-plan`/`ms-test`/`ms-infra`/`ms-mcp`;
- le principe d'orchestration: `ms-mcp` orchestre et appelle les tools de `ms-plan`, `ms-test`, `ms-infra`;
- les SHA a stocker par step;
- les endpoints et tools a exposer pour execution/review;
- le flux de validation guardee `step_validate_with_tests`.
