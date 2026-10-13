# Owner-reported analysis and trial journey correction

## Summary

Correct the existing M4/M5 flow and both open PR#6 review findings on the same branch.
Expose durable prerequisites without changing the paid permission or classic pipeline.

## Why

The owner observed FAILED analysis after successful local sampling and could not
reach trial generation. Fresh failing CI and two review findings justify the change;
no phone HTTP root cause is inferred from screenshots or the separate API probe.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: recorded

- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/OpenAiStyleAnalysisGateway.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisGateway.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisJournal.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleProfileV1.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialJournal.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/data/project/StyleTrialRepository.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrials.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleAnalysisPanel.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStylePanel.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialPanel.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/TargetSelection.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisGatewayTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleTrialJourneyComposeTest.kt`

No schema, permissions, signing, app/package identity, original source coverage,
legacy AI1/AI2 controller or APK build/install path changes. No M6/M7 expansion.

## Behavior before / after

Generic failures become bounded safe categories, and the request schema matches
unchanged local list/text limits. Trials show missing style/targets/stale-context
remediation; STYLE entry appears at the top. CREATE/EXTEND manual text and OpenAI
zero refs work without analysis. Only new separate paid confirmation may submit.
Orphan .new plan reservations do not block readable history; partial .new results
are uncertain and consumed UUIDs stay protected. New trial insertion invalidates
common approval transactionally; idempotent repair preserves existing review.

## Tests actually run

Linux/JDK17/Python3.12: predecessor cfef34e1 hosted production/test compile succeeded,
866 tests/10 failures confirmed in original CI38362969348 and all21 regression
commands38362969202; downloaded ZIP digest matches and full-suite failures read.
Local corrective Gradle command failed before configuration at network download.
Local `python3 -m unittest discover -s scripts/tests -v`:52/52 passed in8.368s.
Final-head hosted evidence follows in QA/PR audit after observation, never
substituted from earlier M5 green runs.
One explicitly authorized synthetic Python Responses probe returned HTTP200,
433 input/185 output/618 total tokens with valid bounded style checks. It was one
POST, no retry, no production Android gateway or image generation, before schema
bounds correction. Official structured-output docs verify the added restrictions.

## Not tested

No physical corrective APK/Flip8/One UI/TalkBack/fold or upgrade/install/provider
image-quality test was run. The owner's previous phone HTTP cause is unproven.
Final hosted correction results are pending publication and must pass before delivery.

## Risks

Device acceptance remains a separate pre-merge owner gate. Trial file/Room recovery
is not distributed ACID; uncertain sent calls may have been charged. Never repeat
paid tests to diagnose generically. Orphan reservations retain bounded history space.
The key was memory-only in a terminated process, absent from source/logs/artifacts.

## Rollback

Use a reviewed source revert while retaining equivalent one-shot/consumed-ID and
approval protections. Room v21 remains; older populated-database downgrades are not
safe. Preserve original app/pack keys and data on signing conflicts.

## Documentation updated

CURRENT_STATE, ARCHITECTURE, HANDOFF, QA, README/CHANGELOG and the existing ADR0005/6
record exact boundaries and limited live evidence. ROADMAP/INDEX retain pending
owner acceptance. Each later meaningful correction needs its own substantive note.

## Follow-up

Publish on the same PR#6 with no force/main/merge operation. Independently count final
XML, retain all21 commands and genuine19/20/21 exports, inspect the actual APK and
update the ten-section audit. Provide exact English phone CTA walkthrough, leaving
owner corrective-device acceptance and merge explicitly pending.
