# Observability & Privacy Dashboard — LocalPDF

## Privacy-First Logging & Monitoring

LocalPDF rejects remote tracking, third-party analytics SDKs, and unredacted logging. All observability is user-centric, privacy-safe, and stored exclusively on the local device.

## The Privacy Dashboard

A central selling point and feature of LocalPDF is the user-facing **Privacy Dashboard** (`:feature:settings`):

```text
┌────────────────────────────────────────────────────────┐
│                   PRIVACY DASHBOARD                    │
├────────────────────────────────────────────────────────┤
│ Documents Uploaded to Cloud:       0 (Zero egress)     │
│ Documents Processed Locally:     184                   │
│ OCR Pages Recognized On-Device:  512                   │
│ Sensitive Fields Redacted:        42                   │
│ Third-Party Trackers:              0                   │
│ Active Network Connections:        NONE                │
├────────────────────────────────────────────────────────┤
│ [Clear Local Search Index]   [Delete OCR Cache]        │
└────────────────────────────────────────────────────────┘
```

## Local Structured Logging

- **Debug Builds**: Timber logger active with log tags formatted by feature module.
- **Release Builds**: Timber tree stripped or routed to a secure in-memory circular log ring (max 100 entries) used only for user-initiated diagnostic bug reports.
- **PII Scrubbing**: Log messages must NEVER contain:
  - Document file paths with real names
  - Extracted OCR text or user queries
  - Passwords, encryption keys, or biometric tokens
