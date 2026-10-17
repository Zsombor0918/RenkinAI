# Keep raw reference identity out of derived silhouettes

## Summary

Correct the new raw-silhouette mapping after the retained identity regression actually fails. This follow-up is limited to local abstract-shape mapping and its existing tests/documentation.

## Why

Source aef0dca0 passes57 focused cases and local Android4/4, but the complete Windows suite executes1010 tests/7 failures. Six are unchanged baseline platform failures; the additional uncertainPaletteCannotForceDarkTileOrGradientAndReferenceIdentityNeverEntersRecipe assertion detects FOREIGN_SOURCE_LOGO entering the generated recipe. No assertion is removed or weakened. That source is a failed checkpoint, not final delivery.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: recorded

`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaReview.kt`, `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaSynthesis.kt`, and `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaWorkflowTest.kt` change. Existing ADR0011 and canonical state/architecture/handoff/QA document the demonstrated correction.

## Behavior before / after

Raw per-image logo_silhouette is evidence about a source image and may include source identity. Local family/global generation rules now accept only six closed abstract construction phrases from that field; unknown text stays in raw reports instead of becoming prompt content. The vocabulary supports compact/rounded/angular masses, connected/open contours and standalone silhouettes. Negated or identity-appended phrases do not match. Standard's separately evidenced abstract drawing rules remain available under the existing support threshold. No NLP classifier, fake precision, changed report schema, additional request or new API behavior.

## Tests actually run

Actual unfiltered aef0dca0 Windows RED is1010/7 with zero errors/skips, retaining the source-identity assertion and all six original platform assertions. Its clean-source emulator3+1 passes; Python64 and range guards pass. Hosted RED checkpoint e839 independently executes1009/5, exactly the new intended quality assertions, with no old Linux failure. Focused identity/overall/reference/version/golden-pack/native-payload correction executes9/9 with zero failures/errors/skips in2m6s. The later complete owned-controller focused run also passes; final committed-head gates remain separate on PR8.

## Not tested

No live/provider/visual-quality or phone test is authorized. A passing diagnostic Android run does not uniquely explain the historical hosted Prepare timeout. Unknown local silhouette phrases remain unestablished rather than guessed.

## Risks

The finite vocabulary is intentionally conservative and may omit useful free-text contours from Quick recipes. Those observations stay inspectable, and supported Standard grammar can express abstract construction. This is not a general guarantee that arbitrary provider prose contains no brand names; the original-first authoritative output contract and human review remain essential. Preserve current schema/journal/consumption/signing boundaries.

## Rollback

Use a reviewed ordinary feature-branch revert or forward repair, preserving v1/v2 readers and consumed IDs. Do not restore raw source-identity transfer as a passing fix or downgrade owner data.

## Documentation updated

ADR0011 explains the raw identity/abstract construction boundary. CURRENT_STATE, ARCHITECTURE, HANDOFF and QA baseline retain the new failed checkpoint and distinguish the small correction from historical Android uncertainty. No milestone is promoted.

## Follow-up

After the focused regression passes, freeze the final source and run complete required checks once. Publish exact-head results/artifact on existing draft PR8, leave owner review and Samsung/live fidelity separate, and stop further speculative emulator corrections.
