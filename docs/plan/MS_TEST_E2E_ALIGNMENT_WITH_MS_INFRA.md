# ms-test E2E Alignment With ms-infra

Objectif:
- `ms-infra` orchestre le runtime E2E, le control plane, le fingerprint et ReportPortal;
- `ms-test` ne porte plus la topologie E2E et ne pilote plus ReportPortal pour les runs infra;
- `ms-test` consomme `runId`, `status`, `reportPortalUrl`, artefacts et diagnostics depuis `ms-infra`.

Endpoints cibles:
- `POST /api/v1/integration/ms-infra/test-run/start-and-poll`
- `GET /api/v1/e2e/runs/{runId}` cote `ms-test` pour la vue agregee QA
- `GET /api/v1/e2e/runs/{runId}` cote `ms-infra` comme source amont
- `GET /api/v1/e2e/runs/{runId}/artifacts` cote `ms-infra` pour l'evidence

Vue agregee retournee par `ms-test`:
- `runId`
- `status`
- `gateStatus`
- `impactedTests`
- `evidence`
- `reportPortalLaunchId`
- `reportPortalUrl`

Regle:
- `ms-test` conserve gate, diff, impacted tests et evidence logique;
- `ms-test` ne conserve pas de compose files ni de manifests runtime E2E.
