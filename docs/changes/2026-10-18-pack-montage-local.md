# Persist automatic pack contact sheets

## Summary

Implement Phase1 local acquisition, automatic adaptive composition and durable
provenance from6e5ff5d2 on the existing branch/PR10. The new UI remains unexposed
until the single multimodal analysis and montage Trial handoff are integrated.

## Why

The product needs one broad pack montage, not another independent1–5-reference
selection. Reuse actual source resources, measured diversity and current private
assets/revisions rather than a new store or a revived DNA analysis framework.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: changed
Durable decision: recorded

`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/PackMontage.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/ReferenceDiagnostics.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/ReferenceImages.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/ReferenceSampler.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/apk/IconCoverageAnalyzer.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/apk/IconPackRepository.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/data/project/ProjectAiData.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/data/project/ProjectAiRepository.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/data/transfer/BackupManager.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/packages/ApplicationManager.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/LocalPackMontages.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/PackMontageTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/ReferenceSamplerTest.kt`

PackMontage/LocalPackMontages implement local artifacts and existing revision JSON.
No Room schema, signing, original editor, AI1/AI2 or batch behavior changes.

## Behavior before / after

Before: up to12 candidates and a separate1–5-reference manual save path. New local
capability: representative up-to30 automatic selection from bounded candidates,
manual content capacity60, balanced adaptive1–4 contact sheets and exact ordered
source member/hash/stamp binding. A page retains256px source detail and transparent
spacing; partial rows are centered with no drawn placeholders. A separate fitted
source render preserves wordmark aspect ratios; historical rendering is unchanged.
Reopen reads stable sheet bytes. Edits rebuild and invalidate dependent analysis;
old revisions remain. Save rechecks owner, source resources and rendered hashes.
Backup rekey marks new evidence for revalidation without rewriting payload bytes.

## Tests actually run

Windows/JDK17: new30-member representative requirement RED1/1 against the old24
limit. Production compilation PASS. Focused ReferenceSamplerTest20, PackMontageTest3,
ProjectAiAnalysisPersistenceTest9 and StyleDnaPersistenceTest3:35/35 PASS in1m59s.
The strengthened image class then3/3 PASS in45s, adding typography and high-entropy
compression stress. Initial fixture compile and non-void JUnit return mistakes
were corrected without claiming those attempts passed. Tests compare all source
pixels/alpha across1,2,7,14,15,16,29,30,31,60 members; rebuild on reorder/remove;
real Room/asset/history/codec/rekey and stale/cross-owner rejection. No provider/key.
Two synthetic30-icon PNGs inspected visually:1440x864 each,75344/76992 bytes;
transparent corners verified and per-icon detail preserved. High-entropy page
exceeds2 MiB while remaining below the real4 MiB input/asset limit.

## Not tested

No live model/style-fidelity or physical-device acceptance. Provider internal
resizing cannot be proved from native request dimensions. Phase2/3 active UI and
network integration remain next work before exposing a default. Final committed
Linux/Android/governance/build evidence follows stable integration, not this stage.

## Risks

Four pages/60 members cap local editing and request memory; initial target is30,
not a fabricated minimum for small packs. All owned page bitmaps are recycled and
inputs are bounded; process-wide heap is not claimed measured. Historical backup
rollback requires retaining the new tagged reader. Old V1/V2 prompt text is untouched.
The historical91e merge-title governance issue stays separate and unresolved.

## Rollback

Use reviewed ordinary reverts/forward repairs, keeping readers/evidence rekey for
any already saved new artifacts. Preserve private PNGs, history, consumed markers
and frozen historical plans; no data reset or migration is needed.

## Documentation updated

ADR0016 records layout/provider evidence and single-artifact identity. Canonical
index, architecture, state, roadmap, handoff and QA explain the local capability,
actual tests and staged unexposed rollout. Current official vision/generation
sources are linked with access date; no model/provider change was made.

## Follow-up

Implement one explicitly consented bounded multimodal analysis using these saved
sheets, then versioned original+montage Trial integration and guided navigation.
Keep the active branch and draft PR10; do not repeat Phase0 or begin batch work.
