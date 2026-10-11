# Codex 02 — Create/Extend and read-only source coverage

## Summary

Implement separate Create and Extend preparation paths on the accepted M1 root,
using real launcher/profile/source catalogs, searchable selectable targets and
bounded actual source preview coverage. Full classic editor remains the destination.
No new AI automation, storage schema or pack pipeline is introduced.

## Why

Create targets are the full launcher inventory, including already edited icons;
Extend must inspect exact external mapping plus draw, independently of profile icons.
The prior instructions/quick-batch filter cannot truthfully provide this distinction.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: changed
Durable decision: recorded

- `app/src/main/kotlin/dev/renkinProject/renkin/MainViewModel.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/apk/ApplicationProvider.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/apk/CoveragePreview.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/apk/IconCoverageAnalyzer.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/apk/IconPackRepository.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/packages/ApplicationManager.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/packages/InstalledAppCatalog.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectHome.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectSetup.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/RenkinAiRoot.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/TargetSelection.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/test/kotlin/dev/renkinProject/renkin/apk/CoveragePreviewTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/apk/IconCoverageAnalyzerTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/packages/CoverageAppFilterTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/CreateExtendFlowComposeTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectShellComposeTest.kt`
- `scripts/run_compose_regressions.py`

The repository reader and parser callbacks are narrow read additions; original
RawElements, duplicate selection, profile controller and classic source writes remain.
ADR 0003 records source/profile separation and user-identity/persistence limits.

## Behavior before / after

Before, Create proceeded straight to the editor and Extend showed instructions.
After explicit completion, Create opens all known launcher targets without a source.
Extend chooses external package then explicit profile; source facts and local state
are separate. Search/select/clear/manual override prepare only UI state. The editor
and old AI/batch/build functions remain accessible; Home never implicitly saves.

## Tests actually run

Fresh Linux/Python 3.12.14 baseline guards passed 45 tests in 7.025 seconds; after
implementation the same 45 passed in 6.912 seconds. Local full Gradle command failed
at Gradle 8.11.1 download with Network is unreachable, before configuration. No
local Android success is claimed. Git diff whitespace and XML resource syntax are
checked before staging; new Android/domain/Compose tests require hosted CI evidence.

## Not tested

New Android tests/build have not executed at this initial checkpoint because the
local Gradle download is blocked. No device/SDK, real pack install, work-user,
Samsung One UI/Theme Park/Flip, process death or live paid provider trial is available.
PR audit stays BLOCKED until fresh hosted Android/regression checks pass.

## Risks

Static small preview certifies drawing only, not animation/themed/OEM completeness.
Synchronous pathological drawable draw can delay cooperative cancellation within
one entry. Existing Room identity lacks user handle; affected rows are disabled
with warning. UI selections are preparation, not durable project metadata or AI plan.

## Rollback

Revert M2 commits to return to the accepted M1 entry flow; no Room migration or
signing/data conversion is involved. Profile rows explicitly created remain normal
Renkin profiles; unsaved VM icons still require explicit save/build before exit.

## Documentation updated

CURRENT_STATE, architecture/root diagram, ROADMAP, HANDOFF, BASELINE and TEST_MATRIX
explain actual behavior and execution limits. INDEX links ADR 0003; ADR 0002 records
actual owner merge. WORKFLOW adds new runner groups while retaining all original
selectors. README and unreleased CHANGELOG describe preparation without AI promises.

## Follow-up

Run fresh feature CI, inspect counts/XML/APK and all required contexts on final head,
then obtain separate owner/reviewer acceptance without automatic merge. Codex 03
may design per-profile style/assets and a small API gateway; user identity migration
needs a separate decision. Main protection review/conversation/strict gaps remain
owner work and Play release gate is unchanged.
