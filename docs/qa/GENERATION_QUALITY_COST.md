# Generation quality and cost evidence - October19,2026

## Owner evidence and limits

The three supplied Samsung JPEG screenshots show PENNY receiving a charcoal rounded
container, Burger King mostly retaining its original arrangement with added gradients,
and AFFiNE receiving a more substantial geometric/color treatment. These are qualitative
observations, not pack-fidelity scores. Actual LUX sheet PNGs, saved style text, original
APK resources, generated RAW PNGs and billing receipts were not supplied. A screenshot
cannot establish native pixel dimensions, real alpha, provider model/quality or file
compression. The apparent checkerboard is the application's preview layer.

The existing original resolver reads the exact component-declared resource in its own
package at device density, never a source-pack icon or thumbnail. It records resource,
type, user, density and platform disagreement; OEM overlays remain possible. Vector
artwork is rasterized at1024; adaptive artwork includes actual platform mask/layers.
A bitmap can have fewer native pixels even though output is1024. Device-density
selection is Android's configuration match, not proof of the largest packaged variant.
Without owner resources, higher-resolution variants and actual source detail remain
unknown; no artificial sharpening/superresolution or source substitution is applied.

Montages retain256px renders in288px transparent cells, usually30 icons/two1440x864
sheets. Actual selected render pixels/alpha, dimensions, bounded memory/bytes, content
recomposition and exact uploaded hashes are tested. Their PNG content is not a UI
screenshot and contains no checkerboard. Larger pages would not recover low-resolution
bitmap detail and could trigger provider resizing. The current layout remains automatic.

Native1024 provider PNGs are decoded/validated/re-encoded losslessly at the same size,
then stored in the existing journal/private asset store. Old AI1/AI2 normalization also
accepts512–4096 inputs; that legacy behavior is unchanged. Trials preserve source and
RAW assets by hash. Preview pixels never feed the provider. Accepted Trial-to-Global
Batch/Apply is not implemented by this optimization; unchanged original editor/export
does not prove those new Trial RAW files were installed on the owner's phone.

## Measurable preview defect

The review decoded128 pixels for a128dp image (and64 for48dp), ignoring display density.
At3x those are displayed at384/144 physical pixels. This introduces avoidable enlargement
even for a genuine1024 result. The regression uses deterministic fine stripes and actual
review decoding at3x, preserving both colors/detail in local PNG evidence. Fix only this
presentation path: use physical destination pixels for bounded decode, filtered display,
and let Compose own published bitmap lifetime. Full input/RAW bytes remain unchanged.

An initial Robolectric captureToImage attempt timed out in its unsupported window-capture
fixture before checking pixels; retain it as fixture evidence, not a demonstrated app
failure. The subsequent native decoder/pixel regression establishes the128-versus384
failure directly. Hosted Android continues exercising actual rendering and restart.

## Prompt scope and historical boundary

V3 already has correct image order, one brief, no legacy contamination and no montage
output permission. Its repeated identity/geometry emphasis can nevertheless encourage
literal logo tracing; the screenshots motivate a clearer future contract, not proof of
a single model failure. Future V4 explicitly treats IMAGE1 as identity rather than a
rendering template, allows reconstruction of proportions/contours, and treats original
platform badges as optional appearance. Standalone versus tiled treatment follows
the montage/reviewed brief, not darkness or an original rounded badge. Unsupported
gloss/shadows remain forbidden; essential brand/text survives genuine restyling.

Use one short authoritative contract and one reviewed editable brief. No new analysis
prompt/schema, family classifier or forced LUX palette. Historical V1/V2/V3 text, plan
fingerprints and model/quality stay readable and unchanged. New V4 freezes effective
text hashes, ordered actual PNGs and model/quality before separate paid consent.

## Quantitative pricing, estimates and unknowns

Checked current official [pricing](https://developers.openai.com/api/docs/pricing),
[generation guide/calculator](https://developers.openai.com/api/docs/guides/image-generation),
[Flare](https://developers.openai.com/api/docs/models/gpt-image-2.5-flare) and
[Sunburst](https://developers.openai.com/api/docs/models/gpt-image-2.5-sunburst).
Standard uncached rates for both2.5 variants: text input$5/M, image input$8/M,
image output$30/M. Equal rates do not establish equal actual token consumption.
The guide's current output calculator groups both2.5 variants and estimates196/439/
1756/3122/7024 tokens at1024 square for low/medium/high/xhigh/max respectively.
Its JavaScript grid formula is reproduced with attribution in the offline calculator;
it is a planning estimate, not measured billing. Actual usage can differ.

Baseline source b9e6556e inherited global settings, whose default was Flare/medium;
9e0f3511 explicitly selected Sunburst/high for new montage plans. The owner's actual
earlier model/quality and six request receipts are unknown. Each separately approved
icon uses n=1,1024x1024 transparent PNG, original plus the same saved sheets (normally
three images total), and one image-edit endpoint. Analysis is separately consented,
saved once and reused. Real retry/regeneration/analysis counts cannot be inferred from
screenshots. The client disables retries and journals endpoint calls, but this review
does not access the owner's installed data. Image usage is currently not retained by
the client; use provider billing/usage receipts for an actual reconciliation.

There is no published2.5 pixel-to-image-input-token formula in the checked docs; the
vision calculator is explicitly NOT a GPT Image input calculator. Do not misapply its
patch budgets or GPT Image1 tile formula. The following complete-request costs use an
EXPLICIT HYPOTHETICAL planning scenario:1000 text input tokens +10000 total image input
tokens per attempt, one1024² original and two1440x864 sheets. Image dimensions do not
prove that token count. No caching, batch discount, analysis, tax, failures or paid
regeneration is included. Fixed inputs cost$0.005+$0.080=$0.085 per attempt.

| Model / quality | Estimated output USD | Total1 icon USD | 6 USD | 30 USD | 100 USD | Relative |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Sunburst high, existing premium | 0.05268 | 0.13768 | 0.82608 | 4.13040 | 13.76800 | 1.000 |
| Sunburst medium, provisional Balanced | 0.01317 | 0.09817 | 0.58902 | 2.94510 | 9.81700 | 0.713 |
| Flare medium | 0.01317 | 0.09817 | 0.58902 | 2.94510 | 9.81700 | 0.713 |
| Flare high | 0.05268 | 0.13768 | 0.82608 | 4.13040 | 13.76800 | 1.000 |

All rows have low confidence as total-dollar forecasts; token rates and arithmetic
are verified, actual image/text usage and acceptance probability are unknown. The
same published output estimate is not a promise that Flare and Sunburst cost the same
per real request. Sunburst medium preserves the model the owner found useful, but
lower output quality may lose typography/detail. Flare may be faster, not necessarily
cheaper or equally faithful. Existing GPT Image1 Mini remains an explicit alternative;
its [documented budget positioning](https://developers.openai.com/api/docs/models/gpt-image-1-mini)
does not establish comparable pack fidelity or identical image-input tokenization.
Do not add another model/endpoint just for an unverified benchmark.

Sensitivity with the same1000 text tokens: at3000 image-input tokens, high/medium
are$0.08168/$0.04217 (48.4% saving); at20000 they are$0.21768/$0.17817 (18.2%).
The central scenario saves28.7%, while output alone saves75%. These assumptions
cannot explain a claimed6–7x increase by model rates alone. Different actual token
usage, older Mini settings, analysis/regeneration counts or billing aggregation could
contribute; none is proven. Reducing prompt text by300 tokens saves only$0.0015 per
request at these rates; reducing visual evidence can damage style and is not adopted.

Direct /v1/images/edits has NO cached-input discount. Responses image-tool inputs
can qualify, but also incur mainline-model tokens; cache hits are not verified by
response usage. No cache savings are counted here. Switching endpoints or introducing
an asynchronous Batch engine is outside this correction and has no demonstrated net
benefit for a few Trials. PNG preserves alpha; JPEG/512 output would break the contract.

Cost per accepted usable icon is per-attempt cost divided by acceptance probability
only as an illustrative expected value. At hypothetical60% Balanced and95% premium
acceptance, costs become$0.16362/$0.14493: premium is better value. No success rates
were measured. Failures/uncertain submissions can still be billed; never auto-retry.

Reproduce: `python scripts/estimate_montage_costs.py --text-input-tokens 1000 --image-input-tokens 10000`. These defaults
are clearly labeled scenarios. Supply real usage totals to calculate a real request;
the output count is still an estimate unless taken from the actual provider receipt.

## Owner visual acceptance, separately authorized

Use the same saved montage revision and edited pack brief for PENNY, Burger King and
AFFiNE. Compare V4 Sunburst/medium versus V4 Sunburst/high, one separately confirmed
result each (six total owner-operated requests); do not silently regenerate failures.
First compare48dp launcher view, then128dp detail, and inspect actual saved RAW PNGs
to distinguish preview scaling from source/output defects. Judge recognition, pack
construction, standalone/tile evidence, optical fill, essential lettering and restrained
depth. Record model/quality, actual usage/cost and accept/reject for each result. Compare
historical V3 results read-only; do not overwrite or reinterpret them. No aesthetic
winner is claimed by offline tests. The agent performs zero live/paid requests.
