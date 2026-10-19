# First Quick DNA local preparation reproduction

## Summary

Add offline regressions to existing PR8 before modifying production. The source
baseline is ecff4e5fa0f075961e28e5fb7e755bb72e418536 on feat/ux-flow-stabilization,
stacked on unchanged PR7 2f422ca8. Main and PR6/PR7 are untouched.

## Why

The owner reports an enabled free Quick scan failing in Step2 on a fresh Flip8
project. Both accessible attachments (645.jpg and647.jpg) were inspected; a third
image was not supplied in this workspace. Source inspection identifies a concrete
path to the same failure: Step1 permits navigation while coverage is Loading,
ProjectShell freezes null currentSourceStamp, and prepareDna rejects the missing
setup stamp before reading references. Its catch replaces that cause with operation.
This is a reproducible candidate, not a recovered phone trace.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysisTest.kt`
adds the first-scan missing-stamp/no-write/no-call diagnostic and valid retry.
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/GuidedProjectRoomJourneyComposeTest.kt`
adds delayed coverage, genuine owned DNA/Room/source-context preparation and
restoration/navigation with separate setup and full-reference indexes.

## Behavior before / after

Production remains unchanged. New assertions require verified source readiness
before guided navigation and a safe source_required diagnostic for missing setup.
A current fresh Quick plan must prepare without a saved style or API key, never
submit, preserve metadata/revisions and survive explicit local rescanning.

## Tests actually run

git diff --check passed locally. Submodules and repository hooks are installed.
The local Gradle8.11.1 wrapper cannot download its distribution: Network is
unreachable, before project configuration. Hosted Android CI and the unchanged
23-command regression workflow will execute the new assertions on this commit.
No actual red result is claimed before inspecting their XML/log evidence.

## Not tested

Physical Flip8, actual Lux resources, paid endpoints, provider style fidelity,
installed-signature upgrades and phone storage faults are not exercised here.
Two supplied screenshots cannot independently distinguish every local exception.

## Risks

This deliberately test-first checkpoint may fail its new assertions. All existing
selectors, assertions,10000ms waits, idling and safety conditions are retained.
Offline gateways fail if free preparation ever submits; no credential is used.

## Rollback

Revert this test-only checkpoint on the existing branch if needed. No database,
asset, history, journal, signing or installed-device state is changed by this work.

## Documentation updated

HANDOFF and BASELINE distinguish the owner failure, candidate path and pending
hosted reproduction from earlier successful CI. Historical evidence remains.

## Follow-up

Inspect genuine hosted assertion failures before production edits. Then correct
only the evidenced prerequisite boundary, run all mandatory final-head checks,
update the existing draft audit and build a fresh APK for owner device acceptance.
