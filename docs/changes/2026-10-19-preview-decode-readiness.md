# Await decoded previews and require current restored montage version

## Summary

Correct demonstrated hosted Linux and Android fixture failures aftereb943a8b. Runtime
preview, V4 request contract, Balanced settings and persistence code are unchanged.

## Why

Hosted CI39184775971 executes937 cases:936 pass and the preview accessibility case
fails on a missing Generated at48px image description. Scrolling materializes the
container but off-main decoding is not tracked by Compose idleness. Its actual Image
semantics are published later, so immediate assertion can precede completion.

Hosted Android39184775979 passes its first three journeys, then restart fails at
an overlooked expected3/actual4 assertion. The restored V4 plan is correct; update
that current-fixture expectation and additionally retain round-trip/model/quality
checks. Historical V3 compatibility remains separately covered, not removed.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleTrialComposeTest.kt`
`app/src/androidTest/kotlin/dev/renkinProject/renkin/offline/StyleDnaEmulatorTest.kt`

Wait for exactly one actual generated-image description with Compose's existing
default wait bound. No sleep, click retry, timeout increase, skipped case or weakened
label/dimension/alpha/paid-count assertion. No production change or new test group.

## Behavior before / after

The fixture now observes completed decoding rather than assuming that a visible
container implies ready pixels. Both original and generated previews retain their
accessible labels, exact48dp size and functional theme/alpha controls.

## Tests actually run

The failed exact-head XML/log artifact12157827073 is downloaded and its GitHub ZIP
digest7168a5b9b993ace38505ba6f0422b6181fefe8e86e3c69cff795d73b8b68ea37
verified. All936 other cases pass, including new density/native/V4/tombstone tests.
Corrected isolated accessibility execution passes1/1 in46s. Android failure artifact
12158315018 has independently checked ZIP digest109f66dad132329f665e98823f1874ce8bb8731491b73c71f4ba21017c9fad2d.
The corrected restart fixture requires exact V4/Sunburst/medium and serialized-plan
equality. Final-head results follow actual execution; the failed source stays retained.

## Not tested

Corrected Android instrumentation Kotlin compiles locally in11s. This is compilation
evidence only; actual corrected-head Android acceptance still requires execution.

No paid/live request, owner key, native owner RAW file or physical phone. No new
visual-quality claim follows from correcting test synchronization.

## Risks

The wait still fails if decoding fails, the label disappears or multiple matches
exist. Historical91e5737d governance and signing/device compatibility remain separate.

## Rollback

Use an ordinary reviewed revert if needed. No runtime state, schema, asset,
provider, request identity or signing change needs to be undone.

## Documentation updated

Current state, QA baseline and handoff retain the failed937-case execution and the
precise fixture cause. The full quality/cost study and V4 decision remain unchanged.

## Follow-up

Commit the successful focused correction, then corrected-head mandatory CI/Android
and independently verified APK. Do not rerun an unchanged failed head or begin
another feature phase. Keep existing PR10 draft/open and unmerged.
