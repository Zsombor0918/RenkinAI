# ADR0017 - prefer editing precision for future montage Trials

Date: October18,2026. Status: owner-authorized targeted correction.

## Evidence and decision

The owner reports weaker in-app style fidelity than a separate manual ChatGPT
experiment. The comparative images and exact real request are unavailable here;
offline tests cannot establish the cause of that aesthetic difference.

Inspecting the actual native multipart request confirms clean V3 assembly: one
authoritative contract and one reviewed brief, no global white-outline style or
historical DNA recipe, true original first, then exactly the ordered saved sheets.
The reviewed effective prompt hash matches the transmitted UTF-8 prompt. The Image
API has a single prompt field, not separate system/developer messages. Provider
internal instructions are outside application visibility.

The remaining confirmed configuration issue is inheritance from global AI settings:
the default is gpt-image-2.5-flare/medium, including for precision-sensitive montage
Trials. Following the owner's conditional authorization, newly prepared montage
Trials now default to gpt-image-2.5-sunburst/high. Explicit existing Trial options
remain available; global defaults, AI1/AI2, manual and historical DNA paths do not
change. No fallback or retry is added when account/model access fails.

Official [Sunburst model documentation](https://developers.openai.com/api/docs/models/gpt-image-2.5-sunburst)
checked October18 describes it as the most capable image-generation/editing model,
intended for editing precision. The [Image API guide](https://developers.openai.com/api/docs/guides/image-generation)
supports edits, transparent PNG and the retained request parameters. High is an
existing supported quality option; no new controls or maximum-quality tiers are
introduced. This choice may increase latency/cost relative to previous settings;
the existing separate confirmation displays the exact frozen model and quality.

## Compatibility and diagnostics

Select the default only at preparation, never at submission. Existing context
serialization already binds model/quality into the plan fingerprint and journal.
Do not change V3 artistic text, V1/V2 reconstruction, stored request IDs or consumed
markers. An old frozen Flare/medium V3 plan still serializes and builds Flare/medium
with its original effective prompt. Changed current options cannot silently submit
an old plan; unchanged consent/revalidation/no-replay rules remain authoritative.

Test-only introspection parses the actual captured multipart into a synthetic JSON
receipt under build/montage-evidence/v3-image-edit-request.json. It records the full
fixture prompt, model/output parameters and image positions/roles/hashes/dimensions,
never Authorization, raw image bytes, owner data or a real key. It neither sends a
request nor installs a production logger. Existing CI PNG evidence retains it.

## Alternatives and limits

Changing the immutable V3 prompt or injecting a model override at the HTTP boundary
would alter stored requests and is rejected. A global model change would affect
AI1/AI2 and is rejected. Extra analysis, another reference pipeline and unbounded
quality settings are outside scope. Prompt contamination was not reproduced; do
not claim it was repaired. Better real visual fidelity remains a hypothesis until
a separately authorized controlled visual test using the same inputs and brief.

Rollback can restore the future preparation default by ordinary review while
retaining historical plan readers and all consumed request evidence. No migration,
key access, live API call, phone operation or signing change is needed.
