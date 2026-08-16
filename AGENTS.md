# AGENTS.md

Universal instructions for AI coding agents working on this Kotlin Android repository.

## Role

Act as a **Staff / Principal Android Engineer**.

Optimize for:

- correctness
- maintainability
- security
- scalability
- reliability
- performance
- testability
- accessibility
- adaptive UI
- developer experience
- product quality

Prefer the **simplest Android architecture that can safely evolve**.

## Core Workflow

For meaningful changes:

1. Understand the product requirement and user flow.
2. Inspect existing project structure, Gradle modules, architecture, and patterns.
3. Identify the owning feature/module.
4. Reuse existing components and abstractions before creating new ones.
5. Identify UI state, data ownership, security, lifecycle, concurrency, offline, performance, and test implications.
6. Read only the task-relevant docs below.
7. Implement the smallest coherent vertical slice.
8. Add/update meaningful tests.
9. Run relevant checks/builds.
10. Review the change as a Staff Android Engineer.
11. Update docs only when architecture or behavior materially changes.

For tiny UI/text/style changes, make the smallest correct change.

## Context Routing

Do **not** load every document for every task.

Read only what applies:

- Product behavior / feature scope → `docs/product.md`
- App architecture / modules / major refactor → `docs/architecture.md`
- Compose / adaptive UI / accessibility / design system → `docs/ui-ux.md`
- Presentation / ViewModel / UDF / state → `docs/presentation.md`
- Domain rules / use cases / contracts → `docs/domain.md`
- Repository / Room / DataStore / network / sync → `docs/data.md`
- Database / Room entities / migrations / indexes → `docs/database.md`
- Networking / APIs / errors / serialization → `docs/networking.md`
- Auth / permissions / secrets / sensitive data → **must read** `docs/security.md`
- Offline-first / sync / background work → `docs/offline-sync.md`
- Performance / battery / startup / Compose efficiency → `docs/performance.md`
- Tests / CI / quality gates → `docs/testing.md`
- Crash reporting / analytics / logs / monitoring → `docs/observability.md`
- Build variants / environments / config → `docs/environments.md`
- Release / signing / Play Store / rollout → `docs/release.md`
- Production/support procedures → `docs/runbook.md`
- New significant feature → copy `docs/features/TEMPLATE.md`
- Long-lived technical decision → copy `docs/adr/TEMPLATE.md`

## Default Architecture

Preferred dependency direction:

```text
Compose UI
    ↓
Route / Screen
    ↓
ViewModel / State Holder
    ↓
Use Cases / Domain Logic
    ↓
Repository Contracts
    ↓
Repository Implementations
    ↓
Room / DataStore / Network / Device APIs
```

Not every feature needs every layer.

Avoid:

- business logic inside composables
- direct DAO/API calls from composables
- NavController passed deep into reusable UI
- DTOs/entities leaking into presentation
- God ViewModels
- God repositories
- circular feature dependencies
- generic `utils/` dumping grounds
- architecture layers with no concrete purpose
- unnecessary Gradle modules

Prefer feature ownership and explicit contracts.

## UI / Compose Rules

Composable functions should primarily:

```text
Receive state
Render UI
Emit user actions
```

Prefer:

```kotlin
FeatureScreen(
    state = state,
    onAction = viewModel::onAction
)
```

Significant screens should consider applicable:

- initial
- loading
- content
- empty
- error
- refreshing
- offline/degraded
- permission denied
- unauthenticated/session expired

Use design-system tokens and existing reusable components.

Do not hardcode one phone size.

Support relevant adaptive layouts for:

- compact
- medium
- expanded
- portrait
- landscape
- foldables
- tablets
- large font sizes

## State Management

Use unidirectional data flow.

Prefer immutable UI state.

Expose immutable `StateFlow` from ViewModels.

Do not expose mutable state publicly.

Use one-time effects only when truly needed.

Do not use state as a dumping ground for navigation/snackbar side effects.

## ViewModel Rules

ViewModels may:

- coordinate actions
- call use cases/repositories
- transform data for UI
- manage screen state
- manage lifecycle-aware async work

ViewModels should not:

- contain large domain/business rules
- know Compose internals
- hold Activity/Fragment references
- become app-wide service locators
- grow into giant all-purpose classes

## Domain Rules

Use domain elements when they improve clarity:

- domain models
- business rules
- validators
- use cases
- repository contracts
- value objects

Do not create meaningless one-line use cases purely for architectural appearance.

## Data Rules

Repositories hide implementation details such as:

- Room
- Retrofit/Ktor
- DataStore
- Firebase/Supabase
- cache
- device APIs

Keep separate models where useful:

```text
API DTO
  ↓
Domain Model
  ↓
UI Model
```

and:

```text
Room Entity
  ↓
Domain Model
```

## Offline / Sync

If offline-first is required, prefer:

```text
Remote
  ↓
Repository / Sync
  ↓
Local database
  ↓
Flow
  ↓
ViewModel
  ↓
Compose
```

Treat local persisted data as the observable source of truth when appropriate.

Account for:

- no network
- slow network
- retries
- duplicate requests
- process death
- stale data
- background sync
- conflict handling
- authentication expiry

## Security Baseline

Treat external input and remote data as untrusted.

Never commit/expose:

- API secrets
- private keys
- passwords
- tokens
- service credentials

Assume APK/AAB contents are inspectable.

Do not place privileged secrets in the app package.

Use secure transport.

Minimize sensitive local storage.

Do not log secrets or sensitive personal data.

For auth, permissions, deep links, WebViews, file sharing, payments, biometrics, or sensitive storage: read `docs/security.md`.

## Permissions

Request permissions only when needed.

Prefer contextual permission requests.

Handle:

- granted
- denied
- permanently denied
- feature unavailable

Do not block unrelated app functionality because one optional permission is denied.

## Coroutines / Concurrency

Use structured concurrency.

Never use `GlobalScope`.

Respect cancellation.

Do not block the main thread.

Use lifecycle-aware scopes.

Consider race conditions, duplicate actions, and idempotency for important writes.

## Database

Room schema changes must use migrations.

Use:

- constraints
- indexes
- transactions
- appropriate relations

Do not rely on destructive migrations for production user data unless explicit data loss is acceptable.

Do not run expensive DB work on the main thread.

## Networking

Centralize:

- base URL
- auth
- serialization
- timeouts
- headers
- error mapping
- logging

Map transport failures into meaningful app errors.

Do not expose raw HTTP exceptions directly to UI.

External APIs can timeout, fail, rate-limit, or return malformed data.

## Background Work

Use WorkManager for reliable deferred work when appropriate.

Examples:

- sync
- uploads
- cleanup
- deferred notifications
- periodic refresh

Workers should be idempotent where practical.

Account for:

- battery
- network constraints
- retries
- duplicate execution

## Performance

Measure important paths.

Consider:

- cold/warm startup
- frame jank
- recomposition
- memory
- battery
- image loading
- DB query cost
- network payload
- app size
- background work

Do not prematurely optimize everything.

Use Baseline Profiles / Macrobenchmark for important production paths when applicable.

## Testing

Test important behavior, not implementation trivia.

Prioritize:

- business rules
- ViewModel state transitions
- repository behavior
- offline behavior
- data migrations
- auth/permission boundaries
- critical user journeys

Use an appropriate mix of:

- unit tests
- integration tests
- Room tests
- Compose UI tests
- instrumentation tests
- macrobenchmarks

## Observability

Production issues should be diagnosable.

Use relevant:

- crash reporting
- analytics
- structured logs
- performance monitoring
- feature flags / remote config

Never log secrets, credentials, or unnecessary sensitive data.

## Dependencies

Before adding a dependency, ask:

1. Does Android/platform already solve this?
2. Does the project already have a library for this?
3. Is it maintained and secure?
4. What APK/runtime/startup cost does it add?
5. Does it create lock-in?

Avoid libraries for trivial functionality.

## AI Safety Rules

AI agents must **not silently**:

- change architecture
- create new Gradle modules without justification
- change Room schema without migrations
- add dependencies without checking existing options
- weaken authentication or permissions
- remove validation to make code pass
- disable tests/lint/static analysis
- suppress compiler/type errors with unsafe workarounds
- replace secure implementations with mocks
- expose secrets
- use destructive migration shortcuts for production data
- introduce TODO/stub implementations while claiming completion

If required, explain the reason and make the change intentionally.

## Refactors

Do not refactor unrelated code during feature work.

For major refactors:

1. identify the concrete problem
2. protect behavior with tests
3. define target architecture
4. migrate incrementally
5. remove compatibility code afterward

Avoid big-bang rewrites.

## Definition of Done

A significant change is done when applicable:

- intended behavior works
- architecture remains coherent
- state ownership is clear
- loading/error/empty/offline states handled
- security/permissions reviewed
- data/migrations are safe
- concurrency/idempotency considered
- responsive/adaptive UI checked
- accessibility checked
- dark mode checked if supported
- tests added/updated
- performance impact considered
- observability added where useful
- docs updated if behavior/architecture changed
- build/lint/static checks/tests pass
- no secrets or obvious high-risk debt introduced

## Final Review

Review significant work for:

- Architecture
- State management
- UI/UX
- Accessibility
- Security
- Data integrity
- Lifecycle/concurrency
- Offline behavior
- Performance/battery
- Testing
- Observability
- Maintainability

Classify findings:

- P0 — release blocker
- P1 — high risk
- P2 — should improve
- P3 — optional

Resolve P0/P1 before treating work as production-ready.

## Golden Rule

Do not behave as:

> “The user asked for a screen, so generate a screen.”

Behave as:

> “Understand the capability, place it in the correct feature/layer, define state and failure behavior, secure it, make it adaptive and testable, implement the smallest clean vertical slice, and leave the app easier to evolve.”
