# Feature Specification — [FEATURE_NAME]

## Purpose

`[PROBLEM_SOLVED]`

## User Story

As a `[USER]`,
I want `[CAPABILITY]`,
so that `[OUTCOME]`.

## Scope

### In Scope

- `[ITEM]`

### Out of Scope

- `[ITEM]`

## User Flow

```text
[START]
→ [STEP]
→ [SUCCESS]
```

## Acceptance Criteria

- Given `[STATE]`
  When `[ACTION]`
  Then `[EXPECTED_RESULT]`

## Navigation

Entry:

`[ROUTE]`

Exit:

`[ROUTE]`

## UI States

- initial
- loading
- content
- empty
- error
- refreshing
- offline/degraded
- permission denied where relevant
- session expired where relevant

## Actions

- `[ACTION]`

## Business Rules

- `[RULE]`

## Data

Reads:

- `[DATA]`

Writes:

- `[DATA]`

Source of truth:

`[ROOM / REMOTE / HYBRID]`

## Repository / Use Cases

- `[CONTRACT]`

## API

- `[ENDPOINT]`

## Room / DataStore

- `[ENTITY/DAO/KEY]`

## Offline / Sync

`[BEHAVIOR]`

## Permissions

`[PERMISSION / NONE]`

## Security

- auth: `[RULE]`
- sensitive data: `[RULE]`
- deep links/intents: `[RULE]`

## Adaptive UI

Compact:

`[LAYOUT]`

Medium:

`[LAYOUT]`

Expanded:

`[LAYOUT]`

## Accessibility

- `[REQUIREMENT]`

## Failure / Edge Cases

- `[CASE]`

## Concurrency / Duplicate Actions

`[RULE]`

## Performance

- expected records: `[VALUE]`
- large lists/paging: `[YES/NO]`
- expensive operations: `[VALUE]`

## Analytics / Observability

- events: `[EVENTS]`
- logs: `[LOGS]`
- crash context: `[CONTEXT]`

## Tests

- unit: `[CASES]`
- ViewModel: `[CASES]`
- repository: `[CASES]`
- Room migration: `[CASES]`
- Compose UI: `[CASES]`

## Definition of Done

- [ ] acceptance criteria satisfied
- [ ] architecture consistent
- [ ] loading/error/empty states handled
- [ ] offline behavior handled where relevant
- [ ] security/permissions reviewed
- [ ] adaptive layouts checked
- [ ] accessibility checked
- [ ] tests added
- [ ] performance considered
- [ ] observability added where needed
