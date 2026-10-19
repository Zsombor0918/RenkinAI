# Faithful style sampling and component originals

## Summary

Incrementally improve local style evidence and exact target-resource provenance on existing PR #6, starting from 422ef578 and two production-identical red checkpoints. No M6/M7, live API calls, new PR, main write or merge.

## Why

Actual red artifacts show coarse hue replacement (11 red-channel becomes 32) and eight-plus outliers replacing a 90% common family. Activity API substitution is simulated independently of EXTEND: old resource loader selects themed A instead of exact alias C. Current code already keeps EXTEND B separate; preserve that working boundary.

## Scope / changed files

`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/OpenAiStyleAnalysisGateway.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/PromptComposer.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/ReferenceImages.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/ReferenceSampler.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisGateway.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisJournal.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialJournal.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialPlan.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/TrialOriginals.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysis.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrials.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleAnalysisPanel.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialBackend.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialPanel.kt`
`app/src/main/res/values/strings.xml`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/ReferenceSamplerTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisGatewayTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisJournalTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialGatewayTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialPlanTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrialsTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleTrialComposeTest.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/PackStyleEvidence.kt`

Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: recorded

ADR 0007 records bounded local evidence, unchanged cloud/V1 style costs and exact resource originals with explicit verification. Room v21, keys, package IDs, profiles, legacy editor, AI1/AI2 and build/install remain.

## Behavior before / after

Local EXTEND preparation uniformly inspects up to 300 mappings (legacy sampler default120 retained). Alpha-weighted real RGB buckets merge in Oklab; geometry density families receive proportional medoids plus minority checks, instead of maximum-diversity domination. At most12 selected/upload images,256px,384KiB each,4MiB aggregate and5MiB HTTP body remain. Sketch retains at most8MiB encoded PNG; evicted selected images must rerender with the same hash and fresh source. Metrics expose observed distributions/roles and explicit unknown stroke/corner/shadow/gradient traits. Model instructions distinguish fixed palette, adaptive native brand colors, mono/duotone, mixed and unknown. V1 model schema and manual editable style stay intact.

Target preparation reads exact enabled current-user ActivityInfo resource (including explicit application-resource fallback), full adaptive layers, density and configuration; no EXTEND/profile/current-display icon is read. It compares the public activity API without claiming pristine OEM art. Optional versioned local original evidence is frozen/journaled and previewed. New resource originals require a plan-specific conscious verification checkbox before the separate paid button; missing resource fails closed. Equal resource/API bytes preserve historical origin stamps; real differences use a v2 origin. Old plans/evidence/approvals stay readable and are not invalidated merely by new optional metadata.

## Tests actually run

Hosted red95cd7786 Android38430246630 and regression38430246744:144 XML,883 tests,4 failures,0 errors/skips. Independently downloaded artifact11831575061,617597bytes,SHA256 b6e5e0af08c68c7c966c9317480da2292e943c164ddd5a84c3083812f8e1dffe. Red812bc7da Android38430840220/regressions38430840226:144 XML,885 tests,5 failures,0 errors/skips; artifact11832170290,621260bytes,SHA256 77d863384c3c68323997aa38bcb02f5620e2852e15f04902aff2fd5ca061bf9c. All failures are new intended regressions. No production change preceded their corresponding observed assertion.

Local Python guards52/52 passed in9.641s at the first red checkpoint. Local complete Gradle and all21 regression commands fail before configuration at8.11.1 download, Network is unreachable; zero Android test/APK claims locally. New final-head compile/full tests/all21/guards and real artifacts are required in the PR audit after execution. Benchmark fixtures compare120/240/300 local caps on50/150/500 mappings; observed numbers are copied from genuine final XML, never inferred from test source.

## Not tested

Physical Flip8/One UI/Theme Park, TalkBack/rotation, actual device install/update/restore, real resource overlays, provider quality and pricing are NOT RUN. Zero live paid calls. Mocked low/high JSON comparison asserts body shape only, no quality or cost improvement; production stays low.

## Risks

Geometry/color thresholds are deterministic heuristics, not calibrated confidence or proof of pack-wide style. One source variant, repeated aliases and a bounded scan can bias observations. Direct package Resources can still be OEM-overlaid; UNVERIFIED_ORIGINAL preview and conscious verification prevent silently claiming pristine Play Store art. A new CI debug key may differ from the installed app; preserve data and original signing identities.

## Rollback

Use a reviewed forward repair preserving Roomv21, legacy origins and the optional evidence parsers. Reverting blindly to a parser that rejects new local journal fields would make those records unreadable. Do not replay paid UUIDs, recolor stored originals, erase old PNGs or downgrade populated DBs. No keystore or package identity changes.

## Documentation updated

CURRENT_STATE, HANDOFF, ARCHITECTURE, QA BASELINE/TEST_MATRIX, INDEX and ADR0007 describe actual boundaries. README/CHANGELOG add original preview verification and local color-policy guidance. ROADMAP keeps M5 pending owner acceptance; no milestone advancement.

## Follow-up

Require final-head CI/governance and a fresh matching debug APK. Owner compares applied pack A, distinct EXTEND B and real target C free of charge, checks local palette roles and summary, then may separately consent to one cloud analysis and one target trial. Require one standalone recognizable C icon with appropriate native colors; keep PR draft/open/unmerged pending explicit owner acceptance.
