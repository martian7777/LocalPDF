# Networking

## Project API

Base URL:

`[BASE_URL_OR_CONFIG_KEY]`

API style:

`[REST / GRAPHQL / OTHER]`

Client:

`[RETROFIT+OKHTTP / KTOR / OTHER]`

Serialization:

`[KOTLIN_SERIALIZATION / MOSHI / OTHER]`

## Centralize

- base URL
- auth headers
- serialization
- timeouts
- interceptors
- error mapping
- debug logging

## API Models

Transport DTOs should not become app-wide domain models by default.

## Timeouts / Failures

External APIs may:

- timeout
- fail
- rate-limit
- return malformed data
- change behavior

Define meaningful failure mapping.

## Retries

Retry only where safe.

Do not blindly retry non-idempotent mutations.

Use backoff when appropriate.

## Authentication

Centralize:

- access token attachment
- refresh logic
- session expiration
- logout cleanup

Avoid feature-specific token logic.
