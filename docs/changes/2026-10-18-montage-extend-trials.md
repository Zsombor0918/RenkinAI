# Connect montage EXTEND to analysis and versioned Trials

## Summary

Finish guided integration from local checkpoints f8d28668 and8a4c5cd0 on the same
PR10 branch. Local acquisition and one-shot analysis are reused, not rebuilt.

## Why

The owner wants one shared visual montage, one practical drawing prompt and a
recognizable original-plus-contact-sheet Trial, with ordinary image-led screens.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: changed
Durable decision: recorded

`.github/workflows/ci.yml`
`app/src/androidTest/kotlin/dev/renkinProject/renkin/offline/StyleDnaEmulatorTest.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/MainViewModel.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/IconGenerationGateway.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/OpenAiImageClient.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/PromptComposer.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialJournal.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialPlan.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrials.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/PackMontagePanel.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectSetup.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStylePanel.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialPanel.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/RenkinAiRoot.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/TargetSelection.kt`
`app/src/main/res/values/pack_montage_strings.xml`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/PackStyleAnalysisTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialPlanTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialTargetSelectorTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectGuidedNavigationComposeTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleTrialComposeTest.kt`
`scripts/run_offline_dna_emulator.py`
`scripts/tests/test_offline_dna_emulator.py`

Preserve original editor, AI1/AI2, Global Batch, Room21/migrations19–21, profiles,
backups, history, signing, original validation, consumed IDs and old V1/V2 readers.
No obsolete service is restored and no Phase0 test is removed in this checkpoint.

## Behavior before / after

Fresh EXTEND automatically saves a source-backed contact sheet revision locally,
then explicitly confirms one analysis request. Browse changes content/order and
automatically rebuilds sheets, preserving previous history and invalidating only
current analysis. Brief/preferences/additional instructions are editable under
Details; existing saved manual styles and CREATE remain in their editor. Larger
image preview and a UI-only alpha checkerboard improve inspection without changing
uploaded PNGs. Continue validates source/member bytes before advancing.

New TrialV3 sends original first, ordered saved montage PNGs next, with one reviewed
brief and explicit contact-sheet/identity/output contract. Optional montage context
fingerprint and prompt hashes bind the frozen identity; historical V1/V2 bytes stay
unchanged. No independent1–5-icon generation selection is added. Random eligible
suggestions are saved once; refresh/manual changes remain explicit and cost-free.

RED controlled suspension proves that keying initial Save on persistReady cancels
its own suspended write when busy changes. Exclude only that self-generated flag
from effect keys; retain source/catalog/route invalidations and optimistic version.
A separate Back RED proves Style returned to stage3 Targets; reset to source summary
stage1. Old UI capacity test now observes the actual random unselected target rather
than assuming fixed p1; disabled-at-three/manual replacement/distinctness/no-payment
assertions remain. CI scheduling stays lean, with PNG evidence upload only.

## Tests actually run

Windows/JDK17: V3 reader RED1/1 and navigation RED1/1 executed; barrier RED1/1
explicitly reports self-cancellation. Earlier naive counter wait did not pump the
Robolectric composition; observe real semantics and inspect state on the UI thread.
Focused10-class invocation executed88 cases:87 passed and one old fixed-position
expectation failed. Its corrected meaningful capacity/replacement case passes1/1.
All new V3/native HTTP/controlled-save/Back cases and selected controller/journal/
coordinator/review cases passed. Debug + Android-test + fixture APK assembly passes.
Strict Python67/67 passes with the configured Git-sh/Python environment; earlier
unconfigured shell attempts are retained failures, not claimed green.

Phase1 checkpoints35 acquisition/Room cases and strengthened3 image cases pass;
Phase2 checkpoint33 native HTTP/reader/journal cases pass. Real MockWebServer proves
separate consent, exact original/montage bytes, prompt-once, incomplete/cancellation/
stale-source recovery and consumed no replay. Actual synthetic30-icon PNGs are
1440x864,75344/76992 bytes;16→15 automatically redistributes2 pages to1.

## Not tested

Final committed-head hosted full Linux, Android3+1,7-case order and APK independent
verification follow this source commit. Local Android journey execution is also
pending, though its source/APKs compile. No live API, owner credential, physical
phone, aesthetic fidelity or Samsung in-place upgrade is tested or authorized.

## Risks

Historical merge-title91e5737d remains a main-range governance blocker, not hidden.
PR10 remains draft/open/unmerged. A new CI debug certificate may differ from older
APKs; inspect the actual public certificate before owner installation. Source
changes, missing resources or imported evidence fail closed. Structured completion
is protocol evidence, never proof of useful live output or matching visual style.

## Rollback

Use ordinary reviewed forward repair/revert without rewriting history or dropping
new data readers. Preserve montage revisions/assets, contract2 analysis records,
V3 request hashes and consumed IDs. Never reinterpret old plans, reset application
data, replace signing keys or retry uncertain requests to facilitate rollback.

## Documentation updated

ADR0016 records the source/layout/analysis/version boundary. Architecture, state,
roadmap, handoff, workflow, index, QA matrix/baseline and new MONTAGE_EXTEND contract
explain current behavior, actual evidence, preserved paths and remaining gates.
Historical checkpoint paragraphs remain historical; final SHA/results belong on PR10.

## Follow-up

Run real emulator acceptance, fix only demonstrated failures, then push the stable
source once for authoritative Linux/build, Python/governance, Android and dispatched
UI-order checks. Independently verify APK/package/certificate/hash and publish the
exact download on PR10. Owner must separately resolve historical integration policy
and approve device/visual acceptance; no batch, subscription or CREATE phase starts.
