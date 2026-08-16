# Observability & Analytics

## Goal

Production should answer:

- What crashed?
- How often?
- On which app version/device/OS?
- Which operation failed?
- How long did it take?
- Can the user recover?

## Project Tooling

Crash reporting:

`[FIREBASE_CRASHLYTICS / SENTRY / OTHER]`

Analytics:

`[FIREBASE_ANALYTICS / AMPLITUDE / POSTHOG / OTHER]`

Performance monitoring:

`[TOOL]`

Feature flags / remote config:

`[TOOL]`

## Logs

Use structured and meaningful logs.

Do not log:

- passwords
- tokens
- private keys
- full auth headers
- unnecessary PII

## Useful Technical Context

Where safe:

- app version
- build type
- OS version
- device class
- feature
- operation
- error category
- duration

## Analytics Events

Track product behavior, not UI implementation details.

Prefer:

```text
item_created
search_completed
sync_failed
```

over:

```text
blue_button_clicked
```

## Performance

Track important:

- startup
- slow screens
- network latency
- sync failures
- crash-free sessions
