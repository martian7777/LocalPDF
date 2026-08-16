# Android Support Runbook

> Fill project-specific dashboards, commands, and escalation once infrastructure exists.

## Crash Spike

Check:

- latest release/version
- crash reporting dashboard
- affected Android versions/devices
- recent feature flags
- Room migration changes
- backend dependency changes

Mitigation:

`[ROLLBACK / FLAG_OFF / HOTFIX PROCESS]`

## Login Failures

Check:

- auth provider status
- token refresh failures
- clock/time issues
- backend auth changes
- deep-link callback configuration

Mitigation:

`[PROCESS]`

## Sync Failures

Check:

- API health
- WorkManager failures
- database state
- auth expiry
- retry loops
- malformed payloads

Mitigation:

`[PROCESS]`

## Migration Crash

Check:

- app version
- previous DB version
- migration path
- affected schema

Mitigation:

`[HOTFIX/FORWARD MIGRATION PROCESS]`

Do not use destructive migration as an emergency shortcut unless product explicitly accepts data loss.

## Performance Regression

Check:

- startup traces
- jank
- memory
- network latency
- image loading
- large DB queries

## Escalation

Owner:

`[OWNER]`

Backend/API owner:

`[OWNER]`

Support contact:

`[CONTACT]`
