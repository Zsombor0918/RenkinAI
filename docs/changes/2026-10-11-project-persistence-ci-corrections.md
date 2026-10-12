# Codex 03 first hosted verification corrections

## Summary

Correct the measured Room/thread and test-harness failures on the first Codex 03
head, and finish fresh read-only target validation and safe reference cleanup.
Commit the genuine KSP-exported v19 schema without editing its JSON or identity.
The application/session/provider architecture and Codex 04–07 boundary stay intact.

## Why

CI 38174665908 and regression run 38174665913 on
`ea5f4032228c2aab5ee9c954e722bb10020a6962` compiled successfully but the complete
JUnit XML contained 710 tests, eight failures, zero errors/skips. The real v18
migration test exposed a synchronous ProfileDao read on the main thread in the
new asset import. Three profile-share cases exposed legacy createProfile switching
to Default inside a suspending Room transaction. The old preview fixture used
legacy Robolectric graphics, which produces a non-real PNG for strict decoding.
Two new selection assertions incorrectly demanded exact text for a substring;
the third inspected metadata before a launched Save coroutine completed. These
are concrete stack/semantic results, not an AlertDialog/idling diagnosis.

Inspection also found target Save using a cached source reader after explicit
rescan, and loaded launcher identities that could miss install/remove while alive.
The fixes reread identity/index only and never call refreshIcons or setPrimarySource.

## Scope / changed files

- `app/schemas/dev.renkinProject.renkin.data.RenkinPackDatabase/19.json`
- `app/src/main/kotlin/dev/renkinProject/renkin/MainViewModel.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/apk/ApplicationProvider.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/ProjectAiRepository.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/transfer/BackupManager.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/packages/InstalledAppCatalog.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectPreparation.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectSetup.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStylePanel.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/TargetSelection.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/project/ProjectAiPersistenceTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/transfer/BackupManagerTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectPreparationTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectSelectionComposeTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleComposeTest.kt`

Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: none

The existing ADR 0004 decisions remain: one profile ID, additive v19, immutable
assets, explicit cleanup and staged backup. The new lease implements those same
file/Room ordering rules and does not introduce another database or session.

## Behavior before / after

Repository file and synchronous legacy DAO operations now run on IO even for a
UI/main caller. Share import uses the existing ProfileDao on Room's transaction
thread; new profile, icons and project rows still commit together. Restore and
explicit GC share the database's existing asset lease, preventing collection
between file finalization and metadata commit. Grouped global-reference copy
inserts only after every file succeeds, and failed/cancelled imports clean their
own newly inserted unlinked rows while retaining prior/history/variant pins.
Explicit removal/discard tracks imported references through restoration.

Fresh launcher identity reads remove uninstalled components, preserve loaded icon
state, and mark newly added or uncertain components unavailable until a real
catalog reload. Resume/package events recheck this screen; Save rechecks again
under the existing profile-session mutex. EXTEND Save requests a fresh read-only
index and rejects changed version/mapping/theme. No background watcher, icon
refresh, paid permission or new generation is added.

The test fixes enable native real PNGs, wait through the existing Compose idle
assertion for actual Save completion, and explicitly request substring matching.
No test, original selector, user interaction, timeout or idling rule is removed.

## Tests actually run

Downloaded artifact 11715826463 from regression run 38174665913, verified its
head/summary and independently counted 127 complete-suite XML files: 710 tests,
eight failures, zero errors/skips. Original cases separately/together/shared form
passed 1/1, 1/1, 2/2, 3/3; Create/Extend 10/10, coverage 28/28 and gateway plus
legacy AI group 31/31 passed. Project UI/data and full suite failed and the APK was
not built. Initial Android run 38174665908 also failed eight tests.

KSP generated schema 19 has identity `7e4b1aec61364b15b862957ac574c8b7` and SHA256
`6c93a2fe875ad2e64482d68dcb72f5c0e1d79892a12f5481145c93440bdb8452`.
It was copied byte-for-byte from that actual build artifact.
`python3 -m unittest discover -s scripts/tests -v` passed 45/45 locally in 24.813 s.
`git diff --check` passed. Follow-up Android execution is pending hosted CI.

## Not tested

No follow-up Android success is claimed yet. Local Gradle download remains blocked
by Network is unreachable. Devices, One UI/Flip/Theme Park, TalkBack, real URI
providers, physical v18 upgrade, backup/keystore and installer/update remain NOT RUN.
No live paid provider request was made; HTTP fixtures use MockWebServer only.

## Risks

The new lease serializes imports/restore/GC per existing Room instance; legacy
watch/prefs/gallery/signing restore is still not a cross-store transaction.
Identity-only scans intentionally cannot certify a new app's local icon state,
so those rows are disabled with an explanation rather than silently selected.
Actual binder/window/device behavior needs the retained manual matrix.

## Rollback

Revert the targeted follow-up for diagnosis only while retaining additive v19 and
known failed-head evidence. A populated v19 database cannot be opened by a v18 APK;
use forward repair or an explicit pre-upgrade backup recovery with signing continuity.
Do not revert schema JSON independently from its entities/migration.

## Documentation updated

CURRENT_STATE and ARCHITECTURE describe IO/lease ordering and fresh launcher/source
validation. HANDOFF records the failing checkpoint and next technical gate.
BASELINE and TEST_MATRIX preserve the failure counts and pending new regressions.
The initial Codex 03 change note remains historical instead of rewriting its result.

## Follow-up

Run the corrected isolated UI/data/legacy groups, original M1/M2 selectors and the
unfiltered compileDebugKotlin/testDebugUnitTest/assembleDebug on this next head.
Keep PR #4 draft and BLOCKED until the technical gates pass, then inspect reviews
and request owner acceptance with device gaps explicit. No merge or release.
