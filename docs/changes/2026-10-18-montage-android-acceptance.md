# Finish native montage acceptance and compact review

## Summary

Close demonstrated integration defects after44c89295 without rebuilding completed
sampling/analysis or changing the V3 payable contract. Keep the existing branch/PR.

## Why

Real emulator rendering, asynchronous resources and lazy lists must be verified
before delivery. The captured screen also showed redundant text above trial images.

## Scope / changed files

Behavior impact: changed
Architecture impact: none
Milestone impact: changed
Durable decision: none

`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/PackMontagePanel.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialPanel.kt`
`app/src/androidTest/kotlin/dev/renkinProject/renkin/offline/StyleDnaEmulatorTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/PackMontageTest.kt`

No schema, provider/model, signing, history, journal, original resolver or batch change.
No test cases/assertions or readiness/billing protections are deleted.

## Behavior before / after

Only guided montage Trials use compact result-first presentation: duplicate project,
selection/count/help sections no longer precede generated images; complete target,
preview and provider/evidence controls remain under Details. Accept/reject/regenerate
and separate approval stay active; old/manual presentation is unchanged.

A native captured-frame RED exposes Canvas drawing a recycled bitmap after successful
analysis Save/reopen replaces the producer byte-array key. The published ImageBitmap
was still retained by Compose while awaitDispose recycled its backing pixels. Clear
the producer's current value and leave published image lifetime to Compose/Android;
only scratch render buffers are manually recycled. The same native capture journey
is the regression, not an artificial assertion or ignored drawing error.

Test-only synchronization waits for actual Browse/consent/restored-style readiness
within the existing15 seconds and scrolls lazy approval status into view. Each input
is dispatched once. Literal127.0.0.1 endpoints match the unchanged socket/cleartext
guards; MockWebServer localhost aliases are not permitted. No network guard is relaxed.
The existing PNG test additionally retains16-before/15-after recomposition files.

## Tests actually run

Windows/JDK17: guided/manual Trial UI26/26 PASS after compact presentation. Debug and
instrumentation APK builds PASS. Native montage journey initially fails on Browse
readiness, then localhost policy and unmaterialized approval; exact failures/traces
are retained. Corrected full journey1/1 PASS in58.374 seconds and separate-process
restart1/1 PASS in20.505 seconds before compact/captured-frame follow-up.

Captured-frame follow-up then reproduces the actual bitmap lifetime error. Corrected
montage1/1 PASS in58.42 seconds, including edit/rebuild, one consented loopback analysis,
Back/forward with durable choices, V3 freeze, decline, second explicit consent, actual
original-first multipart PNGs, fake result, accept and approval. Final local restart
1/1 PASS in23.076 seconds with three saved projects, no new analysis/image request.
Strict Python67/67 local PASS. Native PackMontageTest3/3 PASS in55s, retaining actual before16(two1152x576 PNGs) and after15(one1440x864 PNG) plus unchanged30-icon sheets; pixels/alpha/order and byte bounds remain asserted.

## Not tested

One authoritative full hosted Linux/build plus mandatory final-head Python/governance,
Android3+1 and dispatched order7 still follow this source commit. No paid/live API,
owner key, physical phone, visual fidelity or Samsung upgrade test is authorized.
Screenshots use synthetic fixture artwork and a fake generated circle, not a style score.

## Risks

Historical main-range title91e5737d still blocks integration governance. No waiver or
history rewrite is applied. CI debug certificate continuity is not guaranteed; verify
it independently before installation. Published image native storage is reclaimed
when draw references disappear; bounded render buffers are not total-heap proof.

## Rollback

Use ordinary reviewed forward repair while preserving montage readers/assets,
analysis contract2, TrialV3 and consumed request identities. Do not restore premature
published-bitmap recycling, erase data, change keys or reinterpret historical plans.

## Documentation updated

Current state/handoff, architecture image lifetime, QA matrix/baseline, offline
synchronization and montage acceptance contract describe actual observations and
preserved functions. Index points to PR10 for exact final-head hosted/APK receipts.

## Follow-up

Push this stable source once, run final-head hosted gates, independently verify the
APK package/signature/ZIP and APK hashes, and publish the exact download on PR10.
Keep the PR draft/open/unmerged. Owner integration-policy resolution, certificate-
compatible device acceptance and separately authorized visual pilot remain outside
this implementation; no subscriptions, new batch engine or CREATE redesign starts.
