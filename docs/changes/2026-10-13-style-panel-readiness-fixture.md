# Observe initial style-panel readiness before sampling

## Summary

Repair one measured asynchronous setup race in the retained routed analysis journey.
The test now observes the real enabled prepare action before its unchanged click
assertions. Runtime behavior and the complete21-command runner remain unchanged.

## Why

Closing documentation headffabb37b passed Android38437258752 and898 unfiltered
tests, but regressions38437259108's isolated trial-ui group failed1/23. Actual XML
shows a disabled style-analysis-prepare action, while the panel initial Room load
is independent of the controller/network busy flags awaited by the existing helper.

## Scope / changed files

`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleTrialJourneyComposeTest.kt`

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

Only that existing test and canonical QA/state/handoff/change-note documentation
change. No application, build, schema, API, workflow or hook modification is needed.

## Behavior before / after

Before, the test could assert enablement during the legitimate initial load. After,
it materializes the same node and awaits enabled semantics within the existing
10000ms bound, then retains displayed/enabled/click and every later assertion.
There is no sleep, increased timeout, excluded selector or idling exemption.

## Tests actually run

Actual ffabb37b artifact11832999637 is607386bytes/SHA256
c78c468812b8fa8778c31ba4ebb4127dcd22a9af2a9e74910b256a2a0c793faf.
Independent XML confirms isolated23/1, full144 XML/898/0, every other20 command
passed and19/20/21 schemas match. Local Python52/52 passed7.588s; local Android
and complete runner fail before configuration at Gradle download. This fixture
follow-up still requires its own final-head executed results in the external audit.

## Not tested

No live/paid provider, physical Samsung/Theme Park device, saved-data upgrade,
real signing/install or provider quality test. Local SDK execution remains unavailable.
Previous898 success is not claimed as this follow-up's final execution result.

## Risks

The bounded readiness observation can still fail correctly if the panel never loads.
It does not authorize a request or hide a disabled action. All source-original,
sampling/prompt, legacy bytes, persistence and consent tests remain unchanged.

## Rollback

Revert only this fixture synchronization if evidence justifies another readiness
condition. Preserve original assertions, selectors and time bounds; do not weaken
production loading, consent or source guards to satisfy a setup timing race.

## Documentation updated

QA BASELINE/TEST_MATRIX and CURRENT_STATE/HANDOFF retain the actual isolated failure
and distinguish the independent panel load from controller idle. No durable decision,
runtime architecture or milestone changed, so ADR0007 remains as already recorded.

## Follow-up

Require the new final head's Android/all21/governance success, independent complete/
filtered XML and genuine schemas, then fresh APK verification and updated PR#6 audit.
Leave PR draft/open for owner free A/B/C preview and optional separate paid consent.
