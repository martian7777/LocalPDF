# Release & Play Store

## App Identity

Package/application ID:

`[APPLICATION_ID]`

App name:

`[APP_NAME]`

## Versioning

Version code strategy:

`[STRATEGY]`

Version name strategy:

`[STRATEGY]`

## Signing

Release signing strategy:

`[PLAY_APP_SIGNING / OTHER]`

Never commit signing passwords/private keys into the repository.

## Release Tracks

Use as applicable:

- internal
- closed testing
- open testing
- production

## Rollout

Preferred production rollout:

`[STAGED_PERCENTAGE / FULL / OTHER]`

For risky releases, prefer staged rollout where practical.

## Release Checklist

- [ ] build passes
- [ ] unit tests pass
- [ ] relevant UI/instrumentation tests pass
- [ ] lint/static analysis pass
- [ ] release build uses correct endpoints
- [ ] debug tooling disabled
- [ ] secrets not packaged
- [ ] migrations reviewed
- [ ] crash/analytics tooling verified
- [ ] accessibility smoke check
- [ ] adaptive layout smoke check
- [ ] app startup/performance checked if affected
- [ ] release notes prepared

## Store Listing

Privacy policy:

`[URL]`

Support URL/email:

`[VALUE]`

Data safety requirements:

`[NOTES]`

## Rollback / Hotfix

Process:

`[PROCESS]`
