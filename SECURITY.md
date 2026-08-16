# Security Policy

LocalPDF is committed to building a 100% private, on-device document intelligence platform. We treat the confidentiality, integrity, and privacy of user documents with the highest priority.

## Supported Versions

We provide active security patches for the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 1.x.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

If you discover a security vulnerability or privacy leak (e.g. unintended data egress, cryptographic weakness in vault storage, improper redaction sanitization, or Keystore bypass), please **do not open a public GitHub issue**.

Instead, please report the vulnerability privately via one of the following methods:

1. **GitHub Security Advisory (Recommended)**: Submit a private advisory via [GitHub Security Advisories](https://github.com/martian7777/LocalPDF/security/advisories/new).
2. **Email**: Send encrypted details directly to [security@localpdf.org](mailto:security@localpdf.org) (or project maintainers).

### What to Include

Please provide:
- A clear description of the vulnerability and its potential impact.
- Step-by-step reproduction instructions or a minimal Proof of Concept (PoC).
- Affected Android versions, device models, or specific modules (`:core:security`, `:core:pdf`, `:feature:redaction`, etc.).
- Any suggested remediations or mitigations if known.

### Response SLA

- **Initial Response**: Within 48 hours acknowledging receipt of your report.
- **Triage & Assessment**: Within 5 business days detailing severity classification and timeline.
- **Fix & Public Disclosure**: Coordinated disclosure within 90 days or upon release of a patched version.

## Core Privacy & Security Commitments

1. **Zero Telemetry / Zero Cloud Egress**: Core document processing, OCR, classification, and redaction must never transmit data over the network.
2. **Verifiable Permanent Redaction**: Applying a redaction must permanently overwrite bitmap pixel data and strip underlying vector font/text streams from the PDF object tree.
3. **Hardware-Backed Encryption**: The Private Vault uses Android Keystore `AES-256-GCM` key encryption tied to user biometric authentication.
