# Revalidate relevant Trial inputs without replaying full reads for package noise

## Summary

Keep package invalidations typed and use the existing fresh catalog boundary to
avoid redundant full Trial reloads for packages that change no relevant input.

## Why

Starting source6d7462e6 passes1013 Linux cases but hosted Android38979452071 fails
Standard and dependent restart. Trace records Prepare98.712s, a plan103.582s,
non-source PACKAGE_CHANGED102.830/104.042s and queued reloads102.854/104.063s.
Idle windows103.589/109.816s last under1ms. At113.768s the plan remains, busy=true,
preparation absent; saved reviewed DNA revision1 survives. Restart lacks Standard
completion evidence, not proven lost style. Every package was converted to an
unconditional reload because Trial opts into launcher changes. Those reads repeat
Room/DNA/coverage/original validation even when the target catalog is unchanged.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: none

`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialBackend.kt`,
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectSetup.kt`,
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialPanel.kt`,
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrials.kt`,
`app/src/androidTest/kotlin/dev/renkinProject/renkin/offline/OfflineDnaTrace.kt`,
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrialsTest.kt`,
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleTrialComposeTest.kt`.
Canonical state/architecture/handoff/test matrix document the same boundaries.
No workflow/group, signing, journal, Room/schema19-21 or frozen format changes.

## Behavior before / after

Previously, every observed package event incremented the same reload key as a
resume/theme/explicit refresh. Queued reads always decoded the entire saved style,
validated coverage/approvals and reread originals, keeping controls busy.

Now the unchanged observer still receives all four package actions. Trial receives
the package identity separately; other observer consumers keep the same default.
The same controller queues route-bound package sets. Unconditional requests cannot
be downgraded by later hints. Source and saved-target packages, unknown preparation,
previous errors or a changed fresh catalog still execute full revalidation.
For other packages only, owner.targets freshly reads existing launcher identities,
user ambiguity/local/lock state through the existing provider. Equality with the
current preparation's catalog stamp avoids unnecessary DNA/assets/original reads.
State/route are checked again across suspension. A lookup error clears the plan.
No cached validation, debounce, broadcast suppression or bypassed busy gate.

Plan/staged bytes and approval remain unchanged on benign hints. Decline during a
catalog check cannot resurrect them. Actual target/source/catalog changes retain
current-input, original, immutable reference and paid-consent guards. Lifecycle,
theme, explicit reload, network completion and route handling stay unconditional.

## Tests actually run

Before controller/observer correction, one suspended-Prepare regression executes
1/1 RED: twenty queued benign notifications cause original reads3 instead of2.
The old default backend implementation dispatches the real full load; the retained
trace independently establishes its timing impact. No sleep or widened timeout.
The four focused controller/Trial/observer classes then execute48/48 PASS, zero
failures/errors/skips in8m17s including Kotlin recompilation. Exact committed-head
CI evidence belongs on PR8 after execution. Three new cases are justified; existing1013 cases remain unchanged.

## Not tested

No real key, paid/live request, phone, visual-quality trial or installed-signer
acceptance. Historical package names are deliberately absent from trace; actual
source-match/action and backend sequence are established. Fresh catalog comparison
is the safety criterion, not a guessed system-package allowlist.

## Risks

Selected-target/source events still require potentially expensive full validation.
Real ongoing input changes may correctly keep readiness blocked. Other-package
checks still query the live owner/catalog and hold busy until completion; they
cannot grant paid consent or replay. Hosted Android must verify the real timing.
No claim that Standard visual output or provider completeness is established.

## Rollback

Use an ordinary reviewed feature-branch revert while retaining failing traces,
tests, old readers, frozen plans, consumed IDs and user data. No data migration.

## Documentation updated

CURRENT_STATE and ARCHITECTURE describe typed hints and fresh catalog checking;
HANDOFF records the baseline and bounded stop condition. TEST_MATRIX records the
controlled positive/negative coverage. ADR0012 scheduling remains unchanged.

## Follow-up

Run relevant focused regressions, then one final-head full Linux suite and Android
Quick/Standard/incomplete/cancel/restart. Verify the fresh CI APK independently
and publish URL/hash/source on existing draft PR8. Stop on final acceptance or a
precisely documented blocker; do not begin reference-first or prompt redesign.
