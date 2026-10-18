# Montage EXTEND acceptance contract

The owner vision is implemented on the existing PR10 branch from6e5ff5d2.
Phase0 retirement is not repeated; see [retained/removed inventory](PHASE0_RETIREMENT.md).
No additional individual-reference set is required for new EXTEND projects.
Existing saved manual styles, historical DNA readers and CREATE keep their editor.

## Local visual artifact

CoverageSourceReader supplies actual static source-pack resources. Existing bounded
feature sampling/deduplication chooses up to30 representatives, with proportional
visual-group coverage rather than one assumed palette or alphabetic first entries.
This is a local visual heuristic, not a semantic classifier or proof of aesthetics.
Small packs keep fewer actual distinct resources. Browse edits membership/order,
not pixel sizes, rows or files. Up to60 members fit the bounded manual selection.

Each256-pixel source render fits proportionally inside a288-pixel transparent cell.
At most15 members/page, balanced automatically across up to4 pages; compact grids
and centered final rows.16→15 members automatically changes8+8 into15 on one page.
30 members produce two1440x864 PNGs. The synthetic typography/shape evidence is
75,344 and76,992 bytes; test compares every source pixel/alpha and stresses noisy
PNG compression. One decoded page plus one source icon uses under6 MiB of owned
drawing buffers; this is not a measurement of total application heap. Per-file
input bound4 MiB, selected-byte budget16 MiB. Actual provider preprocessing and
typography understanding remain unverified. UI checkerboards are never encoded
in the saved/uploaded sheets; a larger scrollable preview is available by tapping.

Exact selected component/drawable/hash order, source stamps, sheet hashes/dimensions
and result receipt live in existing revision JSON/private PNG assets. Content edits
create a new revision and clear only the current analysis, retaining history.
Fresh source/resource/hash and owner validation precedes Save and Trial preparation.
Reopening reads the saved sheets; it does not sample, analyze or generate again.

## One multimodal analysis operation

The exact task text is `PACK_ANALYSIS_INSTRUCTIONS` in
[PackStyleGateway.kt](../../app/src/main/kotlin/dev/renkinProject/renkin/ai/style/PackStyleGateway.kt).
It explicitly identifies many independent icons on contact sheets and asks for
practical transformation rules across pictograms, wordmarks, monograms and emblems,
with supported exceptions/uncertainty, without inventing a tile or fixed palette.

One `POST /v1/responses`: gpt-5.4-mini, store=false, stream=false,
reasoning.effort=none, max_output_tokens=4096. Frozen instructions, quoted optional
preferences and ordered `input_image` high-detail data:image/png;base64 inputs
contain the actual saved sheet bytes. Strict response format has just two required
strings: style_prompt and summary. Local bounds6000/240 characters. No families,
per-icon reports or synthesis requests. Native responses must be completed, with
one valid message; incomplete/refused/error/malformed results cannot become current.

Existing journal contract2 fingerprints instructions, image order/detail/context,
stages exact PNGs and consumes the request before submission. Cancellation/restart/
uncertainty cannot replay. A proven received result may recover local Save only.
Preview, editing, Save and false consent send nothing; opening a project never
submits. Model capability/size documentation is linked in [ADR0016](../decisions/0016-pack-montage-inputs.md).

## Separately consented generation

New montage TrialV3 binds the saved montage payload/result fingerprint, exact
reviewed brief, separate instructions, source/original evidence, ordered PNG hashes
and per-target effective prompt hashes. V1/V2 canonical bytes/text remain unchanged.
Old plans are never upgraded or reused. The exact authoritative text is
`montageEditPrompt` in [PromptComposer.kt](../../app/src/main/kotlin/dev/renkinProject/renkin/ai/style/PromptComposer.kt).

Native `POST /v1/images/edits` multipart contains the existing selected model/quality,
n=1, size=1024x1024, background=transparent, output_format=png. `image[]` order:
true target original first, then exactly the current saved contact-sheet PNGs.
The one reviewed pack-wide brief appears once. The contract permits recognizable
geometric redrawing, distinguishes a dark silhouette from an evidenced tile,
preserves meaningful brand colors unless the reviewed style authorizes a fixed
palette, and forbids copying example identities or rendering a montage/collage.
Exactly one balanced standalone target PNG with real outside alpha is requested.
No provider/model change or AI1/AI2 request change is introduced.

Random up-to3 eligible missing targets are saved once with existing ProjectSelection.
Protected/covered/unknown targets are excluded from automatic suggestions; manual
selection and explicit replacement remain. Suggestions grant no paid consent.
The initial Save effect excludes its own busy flag from keys, preserving suspended
Room writes while source/catalog/route changes still cancel/revalidate.

## Evidence and owner acceptance

Focused tests use real Room, native PNGs, fake gateways and loopback-only HTTP.
PackStyleAnalysisTest checks the actual original-first multipart, exact sheet bytes,
one prompt, separate consent, cancellation/source changes and consumed no replay.
The existing Android harness adds montageJourney and retains both manual journeys,
then restartReadOnly in another PID (3+1). Montage journey edits content, analyzes,
navigates Back/forward, prepares V3 and simulates generation/review/approval.
Final exact-head statuses and downloadable artifacts belong in PR10, not inferred
from historical checkpoint totals. CI retains synthetic montage PNGs/metadata.

Normal screens emphasize the pack label, sheets, one analysis action, brief summary
and Continue. Prompt/preferences/source details remain collapsed under Edit Style.
Trial shows originals/results and review controls; evidence/model options/full V3
prompt/sheet previews are under Details. Back from Style returns to source summary,
not another target-selection loop; saved choices survive.

The final APK certificate must be compared to the installed certificate before an
in-place owner-device upgrade. A standalone signed debug build does not establish
Samsung compatibility. Never uninstall, clear data or replace a key to bypass it.
Owner-device acceptance should first inspect local montage editing, navigation,
small-screen controls and restart with existing data. Any real paid analysis or
visual-generation pilot needs separate authorization. Offline success proves
protocol/state safety, not style fidelity, OEM-pristine icons or release approval.
Historical91e5737d commit-subject governance remains separately BLOCKED; preserve
history and let the owner authorize a later safe integration resolution.
