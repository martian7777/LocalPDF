# Offline, Sync & Background Work

## Offline Strategy

Choose:

`[OFFLINE_FIRST / PARTIAL_OFFLINE / ONLINE_ONLY]`

## Source of Truth

`[ROOM / REMOTE / HYBRID]`

For offline-first, prefer:

```text
Remote
  ↓
Sync / Repository
  ↓
Room
  ↓
Flow
  ↓
UI
```

## Sync Triggers

- app foreground: `[YES/NO]`
- manual refresh: `[YES/NO]`
- WorkManager: `[YES/NO]`
- push/event driven: `[YES/NO]`

## Conflict Strategy

`[SERVER_WINS / CLIENT_WINS / VERSIONED / MERGE / DOMAIN_SPECIFIC]`

## Retry Strategy

`[DESCRIBE]`

## Idempotency

Important sync writes should tolerate duplicate execution.

## WorkManager

Use for reliable deferred work.

Define constraints:

- network: `[REQUIRED/NOT_REQUIRED]`
- charging: `[YES/NO]`
- battery-not-low: `[YES/NO]`

Workers should be safe to rerun.

## Process Death

Important unsaved user work:

`[PERSIST / DISCARD / DRAFT]`

## Sync Observability

Track where useful:

- sync started
- sync completed
- sync failed
- records uploaded/downloaded
- retry count
