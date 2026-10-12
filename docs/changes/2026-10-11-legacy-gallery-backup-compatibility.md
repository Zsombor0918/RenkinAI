# Codex 03 legacy gallery backup compatibility

## Summary

Keep legacy full-archive gallery PNG rules separate from the new project-reference
limits, with a real native PNG compatibility regression. Project references retain
their strict four-MiB, visible-artwork contract.

## Why

Fresh source inspection shows getBitmapFromURI's existing power-of-two sampling can
retain raster sides just below 2048 px, and UploadedImageStore.save accepts a fully
transparent bitmap. Applying the new four-MiB/nonempty-reference rule to legacy
uploads would reject an otherwise valid old full backup. This is a source-proven
compatibility gap; fixing it preserves the requested existing Renkin contract.

## Scope / changed files

- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/ProjectAssetStore.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/transfer/ValidatedBackupArchive.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/transfer/ProjectAiBackupTest.kt`

Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: none

ADR 0004's separate private references/legacy gallery and bounded ZIP decision
remains; the correction makes their distinct limits explicit without another store.

## Behavior before / after

Gallery ZIP entries have a bounded 20 MiB allowance sufficient for the existing
sampled raster range. Signature/chunk CRC/terminal/dimension/decoding checks remain,
but a legacy gallery file may be transparent. Project PNGs still default to four
MiB, visible artwork and all ownership/hash checks. The test creates an actual
1300px random-color PNG above four MiB and an empty transparent PNG, restores a
format-3 archive byte-for-byte, then proves both cannot become project references.
The oversized ZIP negative case now exercises the gallery's real twenty-MiB bound.
Credential-exclusion uses an in-memory UUID sentinel, with no committed fake key.

## Tests actually run

`git diff --check` passed. This new compatibility regression is awaiting its own
hosted-head Android result; no unsupported success is claimed. Earlier 02c23aa5
regression XML was 717 tests with one fake-selection failure; same-head original
CI retry also narrowed to that one case after a transient Maven checksum 404.
On 88933c95 compilation has passed and tests are running. Local governance last
passed 45/45 in 21.989 s. No live provider requests were made.

## Not tested

The new archive fixture and complete next-head Android/APK are not yet verified.
Physical devices, OEM, TalkBack, URI providers, upgrade/backup/signing/installer
remain NOT RUN. The existing upload/import code itself is unchanged.

## Risks

Gallery decode may use more memory than a small project reference, but entry size,
4096px dimensions, archive entry count and total 512 MiB remain bounded before
mutation. Legacy restore still spans separate watch/prefs/gallery/signing stores;
project file and final Room ordering remain protected by the existing asset lease.

## Rollback

Revert only the compatibility correction if investigation requires it; that restores
a known old-gallery rejection and must not be called safe legacy support. Retain
v19 forward-repair/pre-upgrade-backup recovery rather than direct APK downgrade.

## Documentation updated

CURRENT_STATE, ARCHITECTURE and ADR distinguish project and legacy gallery limits.
HANDOFF, BASELINE and TEST_MATRIX record the additional compatibility regression
and pending hosted evidence. No historical test result is rewritten as success.

## Follow-up

Verify selection-version diagnostics and the real legacy PNG round trip, remove
temporary probes, then complete final-head original/new groups, complete Android,
APK and governance checks. Keep draft PR #4 BLOCKED until actual evidence exists.
