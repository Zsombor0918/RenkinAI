# Compile concrete overall drawing decisions for generation

## Summary

Continue existing PR8 with a bounded generation-useful overall recipe and a reproduced instrumentation ownership correction. No extra cloud step, schema migration, live request or broad refactor.

## Why

The retained5/5 RED shows missing generation labels, lost synthesized silhouette rules, arbitrary flagged references and absent versioned guidance at the real payload boundary. The hosted diagnostic run passes Android4/4 but reveals Save completing on a worker and repeated local loads. A new Android assertion then reproduces off-main Save return (1 test/1 failure), matching the earlier cross-thread Compose exception. This does not uniquely prove the older hosted Prepare timeout's cause.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: recorded

Production: `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaReview.kt`, `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaSynthesis.kt`, `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/OpenAiStyleDnaGateway.kt`, `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaPlan.kt`, `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaCoordinator.kt`, `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaJournal.kt`.
Tests: `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaWorkflowTest.kt`, `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaGatewayTest.kt`, `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaOfflineHttpTest.kt`, `app/src/androidTest/kotlin/dev/renkinProject/renkin/offline/StyleDnaEmulatorTest.kt`.

## Behavior before / after

The same V1 evidence now compiles explicit background/construction, silhouette, optical fill/padding, edge/stroke, complexity, color, depth, text and negative-space decisions, with separate brand-preservation/abstraction constraints. Unsupported traits appear once instead of repeated generic instructions. Geometry/contour correctly use silhouette observations. Global rules can retain a supported validated synthesis rule only with at least two cited samples and60 percent pack-wide support; conflicting small families cannot impose a universal rule. Raw observations/optional families and __pack__ remain. No dimension is invented from qualitative evidence.

Default references skip explicitly flagged outliers and preserve deterministic construction/type diversity inside the chosen evidence membership. Existing reviewed hashes are not rewritten; immutable Save/plan checks still require member references. Actual multipart tests decode the quoted appearance field before checking its content, preserving native escaping and byte/order assertions. IMAGE1 remains the true target original; subsequent1-5 images are frozen style-only members.

New promptv2 uses the same report/grammar schema, endpoints, model/detail, Quick1/Standard5 maximum and12288 token cap. Its instructions are shorter in characters than v1, without a claim about exact provider token billing. Frozen v1 plans preserve original instruction golden hashes, fingerprints and consumed IDs; v1/v2 results keep their plan version after journal reconstruction. Unknown versions fail closed. Existing saved specifications are not rewritten.

The Android fixture now matches MainViewModel's Main.immediate controller/owner context. Real Room/journal IO remains asynchronous. An explicit Save-return-on-main assertion stays in both success journeys. The default createComposeRule remains unchanged; the rejected StandardTestDispatcher experiment is not restored. No production ownership or trial-controller change follows the unproven historical timeout.

## Tests actually run

Cached Windows JDK17/SDK36 RED: five focused quality tests execute5 failures/zero errors/skips. First correction executes5/2 failures: the prompt lacked literal negative-space guidance and the new assertion inspected an escaped slash without decoding the actual quoted payload. Compact concrete prompt guidance and actual JSON-string decoding correct those boundaries without removing assertions. Subsequent focused run executes6/6, including v1/v2 journal consumption/readback. Expanded focused checks now execute57/57 with zero failures/errors/skips in2m53s, including whole gateway/HTTP/persistence/trial/Compose classes and the four new workflow cases. Final committed-head checks remain separate on PR8.

Test-only thread regression: Standard1/1 fails8.892s with Fixture UI Save must return on Android main like production. After matching controller/owner Main.immediate, the same assertion and complete Standard journey pass1/1 in11.607s. Hosted diagnostic checkpoint e839c227 independently passes Android3+1 with process2959 to4364 and correct loopback counters. Its unit/HTTP quality tests remain intentional RED and its redundant full matrix is cancelled; no cancelled job is a pass. Final committed-source local/hosted evidence remains separate on PR8.

## Not tested

No live/paid extraction or generation, private API key, physical phone, Samsung visual fidelity, billing or token sufficiency is tested. The historical hosted Prepare timeout was not reproduced with logging; its unique cause remains unresolved rather than inferred from the passing trace or threading regression.

## Risks

A provider may still return low-value observations or incomplete output; the parser and consumed markers remain fail closed. Qualitative labels, sample counts and simulated success are not measured visual quality. Logging can perturb scheduling. Existing v2 data needs a compatible forward reader on rollback; never downgrade owner storage, reset markers or replay a request. Classic editor, AI1/AI2, Room21/schema19-21, full/share backups, signing and install paths remain.

## Rollback

Use an ordinary reviewed feature-branch revert or forward repair preserving v1/v2 readability, old saved bytes, originals/history and consumed IDs. Existing frozen trial recipes/current comparisons block changed guidance before submission. No merge, rebase rewrite or force push.

## Documentation updated

CURRENT_STATE, ARCHITECTURE, HANDOFF, QA baseline/matrix and index describe the bounded contract and evidence limits. ADR0011 records compatible version handling, evidence thresholds, defaults and rollback. No milestone is promoted; physical/provider acceptance and owner review remain separate.

## Follow-up

Run final-source focused/full regressions, Android local/hosted, Python/docs/commit checks and verified fresh APK delivery on PR8. Retain BLOCKED if the historical Standard cause remains unresolved; the next single diagnostic step is the same focused Standard selector with these retained traces on a failing hosted execution. Do not spend another speculative correction cycle or weaken assertions.
