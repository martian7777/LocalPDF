# Domain Layer

Use domain abstractions only where they add value.

## Good Candidates

- business rules
- validators
- value objects
- domain models
- repository contracts
- use cases with meaningful orchestration

## Avoid Ceremony

Do not create a use case class for every trivial repository call.

Prefer direct repository usage from ViewModel when the operation has no meaningful business logic and project conventions allow it.

## Domain Independence

Keep domain logic Android-free where practical.

Avoid dependencies on:

- Activity
- Fragment
- Compose
- Android UI classes
- platform storage APIs

## App-Specific Domain Rules

- `[BUSINESS_RULE]`
- `[BUSINESS_RULE]`
