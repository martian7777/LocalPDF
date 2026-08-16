# Environments & Build Configuration

> Fill for each app.

## Build Variants

Recommended:

- debug
- staging
- release

Project variants:

- `[VARIANT]`

## Environment Matrix

| Environment | API | Analytics | Logging | Payments/Integrations |
|---|---|---|---|---|
| Local/Debug | `[URL]` | `[ON/OFF]` | verbose | sandbox |
| Staging | `[URL]` | `[ON/OFF]` | controlled | sandbox/test |
| Production | `[URL]` | production | minimal | production |

## Config Rules

- production secrets never belong in source control
- environment differences should be config-driven
- debug-only tooling must not leak into release
- validate required config during build/startup where practical
- do not silently fall back to insecure defaults

## Application IDs

Debug:

`[APPLICATION_ID]`

Staging:

`[APPLICATION_ID]`

Production:

`[APPLICATION_ID]`

## API Endpoints

Debug:

`[URL]`

Staging:

`[URL]`

Production:

`[URL]`

## Feature Flags

`[STRATEGY / NONE]`
