# Testing & Quality

## Test by Risk

Prioritize:

- business rules
- ViewModel state transitions
- repository behavior
- offline/sync behavior
- Room migrations
- authentication/session behavior
- permissions
- critical navigation/user journeys

## Test Types

Use an appropriate mix of:

- Kotlin/JVM unit tests
- fake repository tests
- Flow/Turbine tests
- Room integration tests
- Compose UI tests
- instrumentation tests
- screenshot tests if the project uses them
- Macrobenchmark

## Behavior Style

Prefer:

```text
Given ...
When ...
Then ...
```

## UI Tests

Prefer semantic selectors.

Use test tags only where semantics are insufficient.

Avoid coordinate-based UI tests.

## Migration Tests

Schema changes affecting production data should include migration verification.

## CI Quality Gates

Relevant work should normally pass:

- compilation
- formatting
- lint
- static analysis
- unit tests
- relevant instrumentation/UI tests
- build

## Project Commands

Fill after project setup:

```text
Format: [COMMAND]
Lint: [COMMAND]
Static analysis: [COMMAND]
Unit tests: [COMMAND]
UI/instrumentation: [COMMAND]
Build: [COMMAND]
All checks: [COMMAND]
```

Coverage is a signal, not the goal.
