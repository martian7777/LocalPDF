# Room / Database

## Project Database

Room database name:

`[DATABASE_NAME]`

Current schema version:

`[VERSION]`

## Entities

- `[ENTITY]`

## Rules

Use appropriate:

- primary keys
- foreign keys
- unique constraints
- indexes
- transactions

Avoid large unindexed scans on hot paths.

## Migrations

All production schema changes require explicit migrations.

Do not use destructive migration for important user data unless data loss is intentionally acceptable.

For risky changes:

```text
add new columns/tables
→ deploy compatible code
→ backfill if needed
→ switch reads/writes
→ remove old structure later
```

## Queries

Consider:

- expected row count
- indexes
- sort/filter patterns
- pagination
- transaction scope

Use Paging 3 for large datasets where useful.

## Testing

Test important migrations and DAO behavior.

Migration tests are required for schema changes affecting production data.
