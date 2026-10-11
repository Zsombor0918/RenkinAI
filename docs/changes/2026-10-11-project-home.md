# Codex 01 — profile-backed Projects home

## Summary

Add unreleased Projects home and explicit asynchronous entry into the full classic
editor. Profiles remain the only identity and session/data owner; no future AI
workflow is added. Base: accepted main `005cffa6d51b11e15c20e68349c0c4ce0f23cd8f`,
after verification of merged PR #1, the P1 fix/tests and resolved thread.

## Why

Users need clear starts for creating and completing icon packs, and safe continuation
of real profiles. The old Unit-returning async methods do not prove load completion;
new navigation must protect unsaved work, old dialogs and special intents.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: changed
Durable decision: recorded

- `app/src/main/kotlin/dev/renkinProject/renkin/MainActivity.kt`: root/intent binding,
  preserving early VM and original URI state key.
- `app/src/main/kotlin/dev/renkinProject/renkin/MainViewModel.kt`: request controller,
  explicit completion, origin/dirty checks in existing mutex, baseline-ready entry;
  legacy public signatures retained.
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/MainScreen.kt`: optional Home,
  shared editor Back, optional toast collector and saved intro dismissal.
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/ProfileSwitcher.kt`: reuse capped
  existing form internally; stable form/action tags.
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectHome.kt`: stateless
  Material 3 profiles, equal actions, honest states/adaptive layout.
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/RenkinAiRoot.kt`: two saved
  routes, per-profile UI primitives, intent/prompt priority and exit protection.
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectSession.kt`: VM-owned
  async state, synchronous reentry gate and interruption without replay.
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/SharedImageIngress.kt`:
  extract the original primitive URI consumption rule for testing.
- `app/src/main/res/values/strings.xml`: English resources and Upload-tab toast
  guidance, without localization refactor.
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectHomeComposeTest.kt`:
  states, identity/accessibility, light/dark, wide/hinge/48dp.
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectShellComposeTest.kt`:
  Back, real child dialog/search, restore, entry priority, Extend/create callbacks.
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectSessionTest.kt`:
  fake async backend, Save/Cancel/origin, completion/failure and one-create/no replay.
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/SharedImageIngressTest.kt`:
  recreation suppression and separately delivered same-URI shares.

ADR 0002 records durable profile identity/single-session/root choices. This is the
complete non-doc scope; build/manifest/CI/guards/schema/storage/AI clients stay intact.

## Behavior before / after

Normal launch used MainColumn; now Projects lists real profiles and opens that same
editor after load/baseline success. Create allocates only after form confirmation
and approved origin save. Dirty different-project/create requests allow Save or
Cancel. Complete explains real source selection with working editor/profile actions.
Editor Back returns Home after child handlers; standalone editor retains double
Back exit. Home alone saves/switches/builds/requests nothing. Dirty exit warns;
clean Back exits normally. Watch/image entry opens Editor, preserving pending
watch and UploadedImageStore/URI guards; the toast explains the Upload tab.
Existing results/installer/refresh/AI routes remain. No fake source coverage is shown.

## Tests actually run

Linux/Python 3.12.14/Git 2.51.1/JDK 17.0.20, October 11, 2026: the offline
`python3 -m unittest discover -s scripts/tests -v` passed 45 tests twice freshly.
`git diff --check` passed. Baseline
`./gradlew compileDebugKotlin testDebugUnitTest assembleDebug` failed before
configuration at distribution download, exit 1, `Network is unreachable`.
No local Android count/build is claimed. Main-merge CI 38138580216 was rechecked
successful as baseline only. Staged/range/PR checks and fresh final-head feature CI
are recorded after execution in [BASELINE](../qa/BASELINE.md) and the PR audit.

## Not tested

Added Android/Compose tests await feature CI because local Gradle/SDK downloads
are unavailable. No full MainActivity Hilt/device session, actual notification/SEND,
backup upgrade/restore, pack update/install, TalkBack, Samsung/Theme Park, fold
lifecycle, physical device/emulator or live paid provider call was exercised.
TEST_MATRIX keeps manual scenarios separate from deterministic fake-state tests.

## Risks

Async/profile/old-editor integration needs device review. Unsaved icons remain
session-only until save/build. Creation/loading is not a cross-store transaction;
inspect discoverable profiles after interruption, never automatically retry inserts.
Saved-state markers prevent replay, not all OS/storage failures. The active ruleset
lacks enforced review/conversation resolution; owner setup/acceptance is outstanding.

## Rollback

Revert Codex 01 through a reviewed PR to restore the MainColumn Activity root.
Default optional arguments preserve standalone legacy behavior. No schema/key/package
rollback is needed; created profiles are ordinary profiles, still available through
the old switcher. Do not delete those profiles or clear app data when reverting UI.

## Documentation updated

CURRENT_STATE/ARCHITECTURE/ADR 0002 describe source and async state diagram.
ROADMAP advances implementation only; HANDOFF/QA record actual tests/gaps/next scope.
WORKFLOW records changed owner rules after #1 merge without altering enforcement
code/settings, INDEX links the ADR, README and unreleased CHANGELOG explain UX.

## Follow-up

Verify all three final-head Actions contexts, review feedback and device gaps;
explicit acceptance/merge belongs to the owner. Enable required review/conversation
resolution. Separate Codex 02 splits flows and analyzes mappings plus drawable
coverage; 03 persistence precedes 04 analysis, 05 trials, 06 staging and 07 edits.
No backend, billing, paid test or additional product feature is part of this PR.
