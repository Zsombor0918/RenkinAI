# Guided project workflow implementation

## Summary

Stage1 implements Project/source, Style, Trials and Completion in the existing local
Android app. Baseline `2f422ca8dbd58174491c954826350bac29bc6dd2` is verified PR7;
draft PR6 and accepted main are preserved. Stage2 fidelity and Stage3 cleanup are plans.

## Why

The owner screenshots exposed competing analysis paths, duplicate Save/Use directions,
raw history and technical identity controls. Test-first native execution reproduced
missing guided navigation, target rehydration and stale trial readiness before source
edits. The two owned-controller tests encountered an earlier Windows journal boundary
and did not initially reach their intended assertions; the
[audit](2026-10-14-guided-ux-audit.md) separates that limitation from source inspection.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: changed
Durable decision: recorded

Presentation and narrow ownership adapters change; persistence/wire/signing contracts
remain. Stage1 code is implemented; device/provider acceptance and later stages remain.

- `.github/workflows/ci.yml`
- `.github/workflows/compose-regressions.yml`
- `.github/workflows/governance.yml`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysis.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrials.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectAnalysisBackend.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectSetup.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleAnalysisPanel.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleDnaPanel.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStylePanel.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialPanel.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectWorkflow.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/RenkinAiRoot.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/TargetSelection.kt`
- `app/src/main/res/values/strings_guided_trials.xml`
- `app/src/main/res/values/ux_style_strings.xml`
- `app/src/main/res/values/ux_workflow_strings.xml`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/GuidedProjectRoomJourneyComposeTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/GuidedTrialComposeTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysisTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrialsTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectGuidedNavigationComposeTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectSelectionComposeTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleGuidedFlowComposeTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleTrialJourneyComposeTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleTrialComposeTest.kt`
- `scripts/check_docs.py`
- `scripts/run_compose_regressions.py`
- `scripts/tests/test_compose_regressions.py`
- `scripts/tests/test_repo_guards.py`

## Behavior before / after

The normal root enables four visible steps while compatible shell consumers retain
their API. A project/source summary leads to one selected style path; fresh projects
recommend Quick, saved aggregate/manual styles keep Legacy. Advanced holds model,
quality, endpoint, hash, grammar and component diagnostics. Current failures are separate
from saved history and per-record validity. Child dirty/saving state locks parent actions.
Immutable style Save synchronizes the parent before continuation, with no duplicate save.

Step3 reuses current saved selections without rewriting them, awaits changed selection
persistence and limits guided targets to one through three eligible named apps. Trial
cards emphasize current results, original/generated previews and explicit review; complete
history remains accessible. All current successes, including older unreviewed variants,
and current ownership/context proof determine approval. Completion shows saved revision,
stored/reviewed trials and approval separately from future Apply and classic build/install.
Cross-profile pending refresh drains only a local load after existing confirmed work.

Tooling adds native batch wrapper invocation, guided tests and stacked workflow triggers.
CRLF Git snapshot normalization preserves strict documentation declarations; LF fixtures
exercise the real unchanged pre-commit whitespace gate. No SDK/dependency/model/schema
or production wire prompt change is included.

## Tests actually run

Native Windows pre-edit tests: four XML/17 tests/11 failures, then three XML/tests/three
failures, zero errors/skips; production/test Kotlin compiled in the first command.
The actual commands and external retained logs are in the audit. Governance reproduced
the CRLF declaration issue and real hook fixture failure before correction; the focused
hook and runner guards subsequently passed without weakening assertions. The complete
native `python -m unittest discover -s scripts/tests -v` then passed all55 tests in
107.949s. Staged documentation audit passed44 changed paths and the proposed commit
subject passed the shared conventions guard. The first integration compiled production
but failed new test compilation on a missing semantics extension import. After that
import fix, 12 XML/98 tests executed with26 failures/0 errors/0 skips; three new fixture
interactions targeted an absent Advanced control in the direct manual fallback, and
the majority of retained controller failures occurred at journal updates on Windows.
All11 new guided trial tests passed. Original reports remain in `ux-green-targeted-1`
and `ux-green-targeted-2` outside Git. These checkpoints do not substitute for corrected
integration or the complete final committed-head checks.

Local final verification uses owner-authorized portable Temurin17, Android API36,
Gradle8.11.1 and Python3.14. The required exact-committed-head commands are
`python -m unittest discover -s scripts/tests -v`,
`python scripts/run_compose_regressions.py` (all23 groups, ending in unfiltered
`gradlew.bat compileDebugKotlin testDebugUnitTest assembleDebug`),
`python scripts/check_docs.py --base origin/feat/style-dna-v1 --head HEAD` and the
commit/PR audit guards. Final SHA, observed integration/final counts, real schema
comparison and fresh APK hash/certificate are recorded externally in the draft audit;
pre-edit failing evidence and historical remote counts are not substituted for them.

## Not tested

Physical Flip8/One UI/Theme Park, emulator/instrumented Android, TalkBack/fold/rotation,
installed saved-data/signature upgrade and real provider style fidelity are NOT RUN.
ADB reports no attached devices. Native Robolectric SDK33/320dp and synthetic providers
are actual local tests but cannot establish phone behavior or artwork quality.

## Risks

Presentation/navigation changes can expose existing timing or device differences.
Real Room/journal/controller journeys and retained regressions guard the demonstrated
seams. Original component identity/IMAGE1, ordered style-only references, historical
AI1/AI2/Stability bytes, immutable originals/RAWs, profiles/modifiers/preferences,
migrations/backups, watch/provenance, calendar/adaptive/clock and app/pack identities
remain. No live requests, automatic retries, user data deletion or phone install occurred.
The new local debug key cannot be assumed compatible with an installed CI build.

## Rollback

Revert this topic branch's source/docs commits after owner review. There is no schema
migration or rewritten saved history; retain all v21/DNA data and consumed journals.
Do not downgrade populated data to an incompatible main APK or uninstall to resolve
signature mismatch. Preserve both signing keys and use a reviewed compatible update.

## Documentation updated

INDEX, CURRENT_STATE, ARCHITECTURE, ROADMAP, HANDOFF and WORKFLOW describe the guided
ownership, implementation milestone and native Windows verification. BASELINE and
TEST_MATRIX separate actual local evidence from historical CI and missing phone gates.
README/CHANGELOG explain the user flow. ADR0009 records the durable presentation and
guard-input decisions. The [owner check](../qa/GUIDED_UX_OWNER_CHECK.md) contains15
concrete acceptance actions and both later-stage plans. Private screenshots remain outside Git.

## Follow-up

The owner requested a bounded usage-limit checkpoint after integration98/26. Shared
Windows AtomicFile replacement is independently reproduced with one native test;
Android production journals remain unchanged. Preserve the branch and finish the
narrow test-environment repair before any broad rerun. The [handoff](../agent/HANDOFF.md)
classifies failures and supplies exact next commands. Complete regression acceptance
is BLOCKED until corrected targeted and all mandatory final-head gates pass.

Keep the stacked PR draft/unmerged for explicit owner review and free phone acceptance.
Stage2 needs a separately authorized fixed-original blind A/B with current pricing and
spend limits; no improved generated quality is claimed. Stage3 is caller-proven cleanup
in small separate PRs. M6 Apply/batch and M7 editing remain planned. Final exact-head
tests and fresh debug APK are independent gates, recorded in the completed PR audit.
