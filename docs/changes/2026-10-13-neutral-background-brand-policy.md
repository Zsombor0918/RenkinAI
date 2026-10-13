# Preserve varying brand accents over recurring neutral tiles

## Summary

Correct the measured neutral-background color-policy failure without changing sampling, remote image/detail/model caps, V1 style schema or persistence identity. Continue PR#6 from73cd29e8.

## Why

Android CI38434288813 compiled897 tests; only the newly added neutral-tile case failed. A white tile covered about90% of visible art and incorrectly implied monocolor, despite varying recognizable chromatic logo accents.

## Scope / changed files

`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/PackStyleEvidence.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/TrialOriginals.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/ReferenceSamplerTest.kt`

Behavior impact: changed
Architecture impact: none
Milestone impact: none
Durable decision: recorded

ADR0007 records background/accent separation within the existing bounded evidence design. The drawable-type label guards AdaptiveIconDrawable with API26, preserving minSdk23.

## Behavior before / after

Meaningful non-recurring chromatic accents in at least half of measured artworks veto fixed/mono classification. Coherent geometry then adapts native target hues, including low-area brand symbols on neutral tiles. Recurring genuine six-color artwork is tested as fixed. Thresholds remain transparent heuristics; uncertain geometry stays mixed/unknown, not forced homogeneous.

## Tests actually run

73cd29e8 Android38434288813: production/tests compiled,897 tests/1 failure. Its only failure is the new test added before this policy change; all prior source/prompt/original/UI assertions passed. Full regression XML/benchmarks are independently inspected when the artifact finishes. Local Android remains blocked before configuration at wrapper download. Final-head Python52/full Android/all21/governance are required after this fix.

## Not tested

No real provider, Samsung device or physical minSdk23 device. Mock and native fixtures prove deterministic boundaries, not vendor overlay immunity, recognition or exact cost.

## Risks

Accent significance/chroma/recurrence are heuristic and bounded to measured source samples. Small unseen accents or alternate variants need human review; the prompt continues preserving native target colors by default. Source/app identity, keys and billable UUIDs remain unchanged.

## Rollback

Keep the null-resource/API guard and compatible local evidence parsers. A reviewed forward policy repair can adjust heuristics while retaining historical styles, images and consent records; never recolor stored originals or replay paid requests.

## Documentation updated

CURRENT_STATE/HANDOFF/ARCHITECTURE/QA and ADR0007 record the actual897/1 checkpoint and neutral/background distinction. No milestone or database migration is claimed.

## Follow-up

Require next-head complete Android and all21 XML counts, genuine schema exports, accurate50/150/500 timing/memory observations, four final governance contexts and matching debug APK. Leave owner free-original/style preview and optional paid one-icon acceptance open before merge.
