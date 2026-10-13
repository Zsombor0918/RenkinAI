# Await the actual local analysis draft before saving

## Summary

Synchronize the remaining real Room analysis-to-trial journey with completed local
reference import. Application source and every previous assertion remain unchanged.

## Why

Head b15112f3 compiled both sources and passed 870 of 871 tests. The one remaining
failure asserts Save enabled while Use is still importing five PNGs in the panel's
coroutine. Controller network busy is already false; it does not prove draft completion.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleTrialJourneyComposeTest.kt`

Only an existing test gains observable-state synchronization and an explicit dirty
draft assertion. There is no production, timeout, idling, selector or runner change.

## Behavior before / after

The test previously scrolled directly to Save after the analysis controller became
idle, even though the UI was still importing reference assets. It now observes
Unsaved style changes before testing Save enabled, retains the no-durable-style
assertion before Save, and then follows the same save/back/trial/restoration journey.

## Tests actually run

Actual b15112f3 Android CI 38370781348 and regression run 38370781359 compiled both
sources and independently ran 871 tests with one failure, zero errors/skips. All
previous nine failures are resolved; Create/Extend passes 10/10, trial UI 20/21.
Downloaded artifact 11799355450 ZIP SHA256 matches
9fe8e55a16d1143134a303c1d350c2412695277a6a826ac0f0675920ae237267;
full-suite XML confirms project-style-save was disabled at the action. Local guard
execution on b15112f3 passed 52/52 in 8.316 seconds. This follow-up's complete hosted
Android/21-command/XML/schema/APK verification is recorded against its actual final
SHA in PR #6; no ancestor count substitutes for it.

## Not tested

Local Android remains blocked before configuration by the unavailable Gradle
distribution download. No physical phone, emulator, paid provider or image-quality
request runs here; the owner's corrective device journey still needs acceptance.

## Risks

The existing 10000 ms condition bound remains unchanged. A failed local import
cannot satisfy the dirty-draft assertion or be masked by a fake style/save. Hosted
Compose synchronization is not evidence of physical-device behavior.

## Rollback

Revert this test-only follow-up through a reviewed PR; application data, Room v21,
keys, source context, consent markers and APK signing remain unchanged.

## Documentation updated

BASELINE retains the measured 870/871 checkpoint and exact artifact digest. HANDOFF
and TEST_MATRIX explain that local Use completion differs from network/controller
completion. No new product or architectural decision is introduced.

## Follow-up

Verify full Android tests/APK and all retained regression selectors on the actual
head, then refresh the PR audit and provide the matching downloadable APK. Leave
PR #6 draft/open/unmerged with owner physical-device acceptance pending.
