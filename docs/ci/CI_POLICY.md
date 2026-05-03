# CI Policy

- `pull_request` vers `main` => `ci-tests.yml` (rapide).
- `push` vers `main` => `ci-main-full.yml` (complet via `run-tests.yml`, `scope=all`).
- `workflow_dispatch` => relance manuelle.

