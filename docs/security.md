# Android Security

Read for auth, sensitive storage, permissions, deep links, WebViews, file sharing, payments, biometrics, or private user data.

## APK Assumption

Assume APK/AAB contents can be inspected.

Never embed privileged secrets in:

- BuildConfig
- strings.xml
- native code
- obfuscated classes
- assets/resources

Use server-side secret storage.

## Network

Use HTTPS.

Do not disable certificate validation.

Only use certificate pinning if the operational trade-offs are understood.

## Tokens / Sensitive Local Data

Minimize sensitive storage.

Use Android Keystore-backed mechanisms where appropriate.

Do not log:

- passwords
- access tokens
- refresh tokens
- auth headers
- private user data
- payment secrets

## Authentication

Define:

`[AUTH_METHOD]`

Centralize session behavior.

Handle:

- logged out
- authenticating
- authenticated
- expired session
- failed refresh
- logout cleanup

## Authorization

Server remains authoritative.

Do not treat hidden UI controls as security.

## Permissions

Only request permissions when necessary.

Handle:

- granted
- denied
- permanently denied
- feature unavailable

Explain permission purpose contextually.

## Deep Links / App Links

Validate:

- route
- arguments
- authentication requirements
- resource authorization

Do not trust deep-link parameters.

## Intents / Exported Components

Minimize exported components.

Validate incoming intents.

Do not expose internal-only activities/services/providers.

## File Sharing

Prefer `FileProvider`.

Do not expose arbitrary filesystem paths.

Use temporary/least-privilege URI permissions.

## WebView

Avoid WebView when not needed.

If used:

- disable unnecessary capabilities
- restrict navigation
- avoid unsafe JavaScript bridges
- validate loaded origins/content

## Biometrics

Use platform biometric APIs.

Do not build custom biometric verification.

## Screenshots / Clipboard

For highly sensitive screens consider:

- screenshot restrictions
- clipboard minimization
- secure input handling

Only when product requirements justify the UX trade-off.

## App-Specific Security

Sensitive data categories:

- `[DATA]`

Compliance:

`[NONE / GDPR / HIPAA / PCI / OTHER]`

Threats to prioritize:

- `[THREAT]`
