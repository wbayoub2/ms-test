# ms-test Docs (V1)

Ce dossier contient les specifications fonctionnelles et techniques pour la solution `ms-test` et son orchestration multi-MS.

Ordre de lecture recommande:
1. [MS_TEST_V1_FEATURES_AND_API.md](/home/machine/Documents/Machine_MS/ms-infra/ms-test/MS_TEST_V1_FEATURES_AND_API.md)
2. [MS_TEST_STEP_VALIDATION_ARCHITECTURE.md](/home/machine/Documents/Machine_MS/ms-infra/ms-test/MS_TEST_STEP_VALIDATION_ARCHITECTURE.md)
3. [MS_TEST_STEP_VALIDATION_API_AND_TOOLS.md](/home/machine/Documents/Machine_MS/ms-infra/ms-test/MS_TEST_STEP_VALIDATION_API_AND_TOOLS.md)
4. [MS_TEST_STEP_VALIDATION_ENDPOINTS_V1.md](/home/machine/Documents/Machine_MS/ms-infra/ms-test/MS_TEST_STEP_VALIDATION_ENDPOINTS_V1.md)
5. [MS_MULTI_MS_STEP_VALIDATION_IMPLEMENTATION_PLAN.md](/home/machine/Documents/Machine_MS/ms-infra/ms-test/MS_MULTI_MS_STEP_VALIDATION_IMPLEMENTATION_PLAN.md)

Regle structurante globale:
- `ms-mcp` orchestre et expose les tools agent.
- `ms-plan` reste domaine plan/OpenProject uniquement.
- `ms-test` porte la logique diff/impact/gate/traceability.
- `ms-infra` porte l'execution des tests et les artefacts.
