# Android Architecture

## Principles

Prefer:

- simple architecture
- explicit ownership
- unidirectional data flow
- feature-oriented organization
- testable boundaries
- incremental modularization
- lifecycle-safe asynchronous work

Preferred flow:

```text
Compose
  ↓
Route / Screen
  ↓
ViewModel
  ↓
Use Cases / Domain
  ↓
Repository Contract
  ↓
Repository Implementation
  ↓
Room / DataStore / API / Device API
```

Not every feature needs every layer.

## App-Specific Architecture

```text
[HIGH_LEVEL_ARCHITECTURE_DIAGRAM]
```

## Suggested Project Shape

```text
app/

core/
  common/
  model/
  designsystem/
  ui/
  network/
  database/
  datastore/
  analytics/
  testing/

feature/
  [feature]/
```

Do not create every folder/module unless needed.

## Gradle Modules

Current modules:

- `[MODULE]` — `[RESPONSIBILITY]`

Keep as packages until module boundaries provide a real benefit such as:

- build performance
- ownership
- dependency isolation
- reusable platform infrastructure
- dynamic feature delivery

## Dependency Rules

- `[PROJECT_SPECIFIC_RULE]`

## Navigation

Navigation ownership:

`[DESCRIBE]`

Prefer type-safe routes where supported.

Reusable components should not receive `NavController`.

## State Ownership

Screen state should usually be owned by a ViewModel/state holder.

App-wide state should be introduced intentionally.

## Major Change Checklist

Before major architecture changes:

- What concrete problem exists?
- Can current architecture support it?
- Is a new module actually needed?
- What migration cost exists?
- Is the choice reversible?
- What is the test/build impact?
- What happens as feature count grows?

Use an ADR for significant decisions.
