# Component resource nullability and neutral-tile regression

## Summary

Handle the nullable resource result explicitly and add focused source/neutral-tile regression cases. Continue existing PR#6 from ab80db86.

## Why

Android CI38433644576 compiled no Android tests: TrialOriginals.kt:61 passes nullable Drawable to the renderer. A neutral tile can dominate palette area while meaningful logo accent hues vary; test that case explicitly before refining policy.

## Scope / changed files

`app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/TrialOriginals.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/ReferenceSamplerTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialPlanTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrialsTest.kt`

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

Null resources already belong to the documented fail-closed contract; this is compile repair, not a new fallback. Existing assertions/selectors are preserved.

## Behavior before / after

Null drawables raise UNVERIFIED_ORIGINAL rather than entering a non-null renderer. New tests reject missing resources, disabled/wrong-user aliases, and vary actual saved B2 reference bytes while C stays constant. The new neutral-tile policy assertion may be red; no policy change precedes its observed result.

## Tests actually run

Local52 guards passed7.857s on ab80db86; committed range docs93 paths and conventions15 subjects passed. Local Gradle/all21 remain blocked before configuration at download. Hosted ab80db86 production compile failed, tests/APK skipped. Next-head full and focused execution must establish actual assertions.

## Not tested

No live API, phone or Samsung overlay/installer test. Synthetic native resource fixtures cannot certify vendor APIs or model quality.

## Risks

Fixed/brand color policy is heuristic; a common white tile is not evidence that every logo should become white. The source-only byte invariance tests must preserve distinct current style refs as well as original-first ordering.

## Rollback

Keep explicit nullable handling and all original tests. Revert only invalid fixtures after evidence; do not add themed/source-pack substitution, replay requests or alter Room/signing identities.

## Documentation updated

CURRENT_STATE/HANDOFF/ARCHITECTURE/QA record the exact failed compile boundary without claiming green Android results. No durable decision changes.

## Follow-up

Read the next hosted assertions, repair any measured neutral/background misclassification, then exact final-head full/all21/governance gates and APK. Owner original/color/one-icon device acceptance remains open.
