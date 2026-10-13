# Single-icon OpenAI trial output correction

## Summary

Restore the one-target/one-output contract for M5 OpenAI trials on the existing
feat/style-trials-m5 / PR #6. Corrective source baseline is a1c2310; executable
test-first head is 44c69cb2cab7722db93d26464b9b3e2b334f04a8. No M6/M7 work.

## Why

Owner ticket6142499049 reports multi-app collages on the a1c2310 Flip8 APK using
saved Basit MIXED style. M5's raw prompt override replaced the stronger legacy
iconPrompt. The missing output contract is verified by five failing regressions;
the extent of its contribution to model artwork remains an owner-device acceptance
question. No live provider request or screenshot inspection is fabricated.

## Scope / changed files

- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/PromptComposer.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/OpenAiImageClient.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/IconGenerationGateway.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialCoordinator.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialPlan.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialGatewayTest.kt`

Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: recorded

One shared OpenAI contract at the final request boundary, used also for input/plan
validation. The MIXED fixture now saves genuine reviewed typed AUTO_ANALYSIS
evidence, with the same contract assertions retained. No schema, dependencies,
legacy settings, Stability transport, journal, UI, runner or signing change.

## Behavior before / after

Before, any OpenAI override could omit image roles and single-output constraints.
Now image1 is the single TARGET ORIGINAL; images2..N transfer palette/stroke/optical
size/geometry/shading/padding only, never their app symbols. Output is exactly one
centered1024x1024 transparent launcher PNG, forbidding sheets/grids/collage/montage/
mockups/labels/watermarks/checkerboards. Intrinsic target-logo typography can survive
when essential to recognition. Source brand motifs are observations, not subjects.

Saved style/project/app text is quoted appearance data below authoritative rules.
M5 avoids nested quoting to preserve maximal6000/2000-character saved inputs; final
escaped prompt remains bounded by32000. Gateway preflight rejects expansion before
any endpoint marker. The native multipart factory always assembles the same helper
for a non-null OpenAI override; raw overrides cannot replace the hard contract.
Null overrides keep historical iconPrompt bytes; Stability keeps composeSaved and
its own init_image/style_image/two-endpoint contract. Old PNGs/reviews/approvals,
style categories, selections and paid-request history remain untouched.

## Tests actually run

Before production edits, Android CI38379257399 compiled and ran879 tests/6 failures;
five new contract assertions plus one existing analysis-journey UI assertion. No APK.
Independent all21 regression run38379257536 compiled and executed144 full-suite
XML/879 tests/5 failures/0 errors/0 skips, precisely the five new contract cases;
the existing21 trial-UI cases all passed. Legacy AI1/AI2 and Stability parity passed
in14 AiImagePipelineTest cases; retained generation/legacy group33 passed.
Downloaded artifact11802239145 SHA256 is
bf5d18151c46802f37caf3434a5973dc59ac485246c94717b7ff49a90bcc72fb.
Initial b3d5a6bf failed test compilation; its import-only correction is separately
recorded and not counted as red assertion proof.

Local Linux/Python3.12/JDK17: python3 -m unittest discover -s scripts/tests -v
passed52/52 in6.919s. Local python3 scripts/run_compose_regressions.py attempted
all21 commands with zero executed Android tests: Gradle8.11.1 download was blocked,
Network is unreachable. The local complete Gradle command fails at the same boundary.
Whitespace passes. Staged/range docs and commit guards run before publication;
complete final-head hosted Android/all21/XML/APK/governance results are recorded
in the PR audit after observing the real committed SHA, never copied from871 history.

## Not tested

No local Android execution, phone/emulator, account/model quality, live or paid API
request, actual OEM install/update/Theme Park, real storage/process-kill or signing
continuity test. PNG validation is structural and cannot detect collage or semantic
identity. Final artwork acceptance requires a separately authorized owner generation.

## Risks

A clearer prompt does not guarantee model obedience or visual identity. Keep PR#6
draft/open until the owner verifies one new icon at48px, no reference logos and
credible alpha/padding. Stop on another collage. Existing bad paid variants must be
explicitly marked Not suitable; no automatic regeneration/relabeling/acceptance.
Native request fields/order/options/n=1 remain unchanged. Full existing UI/editor/
profile/backup/migration/build/sign/install and one-shot failure suites remain required.

## Rollback

Use a reviewed source revert retaining equivalent output/consent protections.
Room staysv21; populated downgrade to20 is unsafe. Preserve original app/pack keys,
all owner data and consumed request markers; never replay old paid requests.

## Documentation updated

CURRENT_STATE, HANDOFF, ARCHITECTURE, QA BASELINE/TEST_MATRIX, INDEX, ROADMAP,
README and CHANGELOG describe the new output boundary and open owner gate.
ADR0006 records provider-specific mandatory OpenAI assembly and unchanged Stability/
legacy bytes. Milestone remains implemented with owner review/merge pending.

## Follow-up

Verify every final-head check and independently count actual XML/skips, compare
genuine19/20/21 schema bytes, then download/verify the matching debug APK. Record
exact SHA/artifact evidence and one audit result in PR#6. Owner: retain the saved
Basit style, reject the old bad trial, choose one PENNY/Sketchbook/Gallery Assistant
target, inspect frozen inputs and separately confirm a new paid request. Accept only
one recognizable target icon; shared approval and explicit merge decision are later.
