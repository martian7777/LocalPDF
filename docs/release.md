# Release & Distribution Strategy — LocalPDF

## Distribution Channels

1. **Google Play Store**: App Bundle (`.aab`) with Play Asset Delivery for optional language packs.
2. **F-Droid / GitHub Releases / Direct APK**: Standalone universal `.apk` or per-ABI `.apk` (100% Google Play Services free).

## Pre-Release Checklist

- [ ] Run full automated test suite: `./gradlew test`
- [ ] Run static analysis & lint: `./gradlew lintRelease detekt`
- [ ] Verify Room database schema export has not broken migration paths
- [ ] Test fresh install and migration from previous release on physical test device
- [ ] Verify 100% offline functionality (Airplane mode scan, OCR, search, redact, and export)
- [ ] Verify camera preview and OpenCV contour rendering on multiple aspect ratios
- [ ] Verify BiometricPrompt authentication and Private Vault decryption
- [ ] Check APK size per ABI (ensure INT8 ONNX models are compressed appropriately)
- [ ] Verify Baseline Profiles are compiled in release AAB for sub-300ms cold startup

## Signing Configuration

Release keys must never be committed to Git. Managed via environment variables in CI/CD (GitHub Actions):
- `LOCALPDF_KEYSTORE_BASE64`
- `LOCALPDF_KEYSTORE_PASSWORD`
- `LOCALPDF_KEY_ALIAS`
- `LOCALPDF_KEY_PASSWORD`

## Local production build

The keystore must live outside the repository. Set `LOCALPDF_KEYSTORE_PATH`,
`LOCALPDF_KEYSTORE_PASSWORD`, and `LOCALPDF_KEY_PASSWORD`; the alias defaults to
`localpdf-release` and can be overridden with `LOCALPDF_KEY_ALIAS`.

Run `gradlew :app:collectReleaseArtifacts`. Outputs are copied to
`app/build/outputs/release/localpdf-0.1.0-release.aab` and
`app/build/outputs/release/localpdf-0.1.0-universal-release.apk`.

Back up the keystore in two encrypted, user-controlled locations. Losing it can
prevent future direct-install upgrades. Never commit it, its passwords, or a
password-bearing command transcript.

## GitHub Actions secrets

### Bootstrap entirely in GitHub Actions

No local JDK or Android Studio is required. Before running the bootstrap, create
these temporary repository secrets:

- `LOCALPDF_SECRET_BOOTSTRAP_TOKEN` — a fine-grained personal access token scoped
  only to this repository, with repository **Secrets: Read and write** permission
- `LOCALPDF_SIGNING_BOOTSTRAP_PASSWORD` — a unique password of at least 16
  characters that is stored in a password manager

Run **Actions → Bootstrap Release Signing → Run workflow**, enter `CREATE`, and
wait for it to finish. Download the `localpdf-release-keystore-BACK-UP-NOW`
artifact immediately; it expires after one day. Store the keystore in two
encrypted locations together with the signing password.

After confirming the backup, delete both temporary bootstrap secrets, revoke the
fine-grained token, and delete the bootstrap workflow run so its short-lived
keystore artifact is removed. The workflow creates the four permanent secrets
listed below. Never rerun it for an app that has already been distributed unless
you intentionally want a new signing identity.

### Manual setup

In GitHub, open **Settings → Secrets and variables → Actions → New repository
secret** and create:

- `LOCALPDF_KEYSTORE_BASE64` — the complete keystore encoded as one Base64 value
- `LOCALPDF_KEYSTORE_PASSWORD`
- `LOCALPDF_KEY_PASSWORD`
- `LOCALPDF_KEY_ALIAS` — optional; defaults to `localpdf-release`

PowerShell can prepare the Base64 value without modifying the keystore:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes('C:\secure\localpdf-release.jks')) |
  Set-Clipboard
```

Run **Actions → Build & Publish Artifacts → Run workflow**, choose `all`, and
open the completed run. The job summary links to the run's **Artifacts** section;
the artifact ZIP contains the APK/AAB and `SHA256SUMS.txt`. A pushed `v*` tag also
attaches the same files to GitHub Releases.
