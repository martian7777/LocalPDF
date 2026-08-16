# Kotlin Android Enterprise AI-Assisted Engineering Template

Reusable engineering guardrails for professional Kotlin Android development with AI coding agents.

## Start Here

Fill project-specific placeholders in:

- `docs/product.md`
- `docs/architecture.md`
- `docs/ui-ux.md`
- `docs/data.md`
- `docs/database.md`
- `docs/networking.md`
- `docs/security.md`
- `docs/environments.md`
- `docs/release.md`

Then:

1. Adapt `Makefile` to your project commands.
2. Adapt CI workflows to your Gradle modules and build variants.
3. Copy `docs/features/TEMPLATE.md` for every significant feature.
4. Use ADRs only for meaningful long-lived decisions.
5. Keep `AGENTS.md` as the canonical AI engineering instruction source.

## Suggested Default Stack

Unless the project has a strong reason otherwise:

```text
Language: Kotlin
UI: Jetpack Compose
Design: Material 3
Architecture: layered + feature-oriented
State: UDF + StateFlow
Async: Coroutines + Flow
DI: Hilt
Database: Room
Preferences: DataStore
Networking: Retrofit/OkHttp or Ktor
Serialization: Kotlin Serialization
Navigation: Navigation Compose
Background work: WorkManager
Testing: JUnit + fakes/mocks + Turbine + Compose UI tests
Static analysis: Detekt + ktlint
Performance: Macrobenchmark + Baseline Profiles
Build: Gradle Kotlin DSL
CI: GitHub Actions
```

Architecture should remain simpler than this if the app does not need all of it.
