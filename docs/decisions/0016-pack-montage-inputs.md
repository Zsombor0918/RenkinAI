# ADR0016 - use one persisted pack montage for visual style

Date: October18,2026. Status: owner-authorized Phases1–3; montage, one-shot analysis and TrialV3 integration implemented; final gates pending.

## Decision

Reuse CoverageSourceReader, feature/geometry-proportional ReferenceSampler and existing
private PNG assets/StyleRevision JSON. Add a bounded pack_montage payload containing
ordered source component/drawable/hash members, source/setup/reference stamps and
ordered sheet hashes/dimensions. A small owner/payload/brief-hash evidence object
supports existing backup rekey/revalidation. No Room schema, migration or duplicate
reference store. Source member PNGs are rendered and verified during editing; actual
sheet PNGs remain the durable primary visual artifacts even if a pack is unavailable.

Automatic sampling chooses up to30 diverse representatives from a bounded pool;
manual content editing can use up to60 within four sheets. Historical sampling and
source rendering retain their old limits/bytes. Montage rendering fits nonsquare
resources proportionally. The same rendered PNGs, never another1–5-icon selection,
will supply analysis and future generation.

## Layout and bounds

The application owns all layout choices. A source icon retains its full256-pixel
render inside a288-pixel cell with transparent spacing. A page has at most5 columns
and3 rows; capacity15 preserves source detail without rescaling. For any member
count, balance contents across the necessary pages, choose a compact grid and center
a partial final row. There are no empty-slot drawings, labels, opaque page fill or
checkerboard pixels. Removing16 to15 members automatically changes two balanced
pages to one;30 gives two1440x864 PNGs. The general policy permits up to4 pages
rather than shrinking an edited larger sample to force two files.

At1440x864 a decoded page is4,976,640 bytes; only one page bitmap and one decoded
source icon are drawn at a time. Each page must fit the existing4 MiB asset/native
input limit; the selected candidate and sheet byte budgets are bounded. High-entropy
fixtures exercise compression limits. This bounds owned buffers, not all Android
process heap allocations or provider-internal preprocessing.

## Provider evidence and limits

Official [vision documentation](https://developers.openai.com/api/docs/guides/images-vision)
checked October18 supports PNG image inputs; gpt-5.4-mini high detail currently has
a2048 maximum dimension and2500-patch resizing budget. A1440x864 sheet needs1215
32-pixel patches, so our native input is below those limits. Request count alone
does not establish cost. The existing image models/settings are preserved; see
[image-generation documentation](https://developers.openai.com/api/docs/guides/image-generation).
Provider-internal resizing, typography interpretation and style quality are not
proven by file dimensions. No paid request or owner-key access was used to verify docs.

## Invalidation, compatibility and rollout

Local content edits rebuild the sheets automatically and discard the draft's
current analysis result when its content fingerprint changes. Save creates another
ordinary revision, retaining prior paid results/history. Fresh source stamps,
resource names and rendered hashes are checked again before save. Cross-profile,
missing or stale source bytes fail closed. Existing historical codecs and V1/V2
Trial bytes remain immutable. The new foundation stays unexposed until the single
analysis and montage Trial route are connected; future generation needs its own
version. Rollback must retain the new saved-data reader and evidence rekey path.

## One-shot analysis boundary

Use gpt-5.4-mini through Responses with high-detail saved sheet PNGs, store=false,
stream=false, reasoning effort none and4096 output-token cap. One request asks for
only style_prompt and summary strings, then strict local bounds validate them.
The model's image input/Responses/structured-output support and current token
pricing are documented on its [official model page](https://developers.openai.com/api/docs/models/gpt-5.4-mini).
No dollar-price guarantee is hardcoded; consent describes one potentially billable
operation whose input/output tokens determine cost. The streaming request body
holds base64 for one PNG at a time. Existing journal contract1 stays byte-compatible;
contract2 adds a fingerprinted version/detail distinction and small plain-text
result. Epochs, UUID consumption, cancellation uncertainty and no retry remain.
Successful result/local Save failures can recover the received candidate without
repeating the request, only while its source/owner/revision still match.


## Future Trial contract and presentation

Only newly prepared montage requests use generation V3. The optional montageFingerprint
binds the entire saved payload/result provenance; effective per-target prompt hashes
bind the exact single reviewed brief and additional instructions. Original PNG first,
then ordered saved contact-sheet PNGs, without another individually selected reference
set. V3 explicitly names contact-sheet roles and forbids reproducing example identities
or a montage. Original recognition permits meaningful geometric redrawing. Native
n=1,size=1024x1024,background=transparent,output_format=png stay unchanged.
Historical V1/V2 canonical bytes, text and fingerprints are not upgraded; their consumed
IDs cannot be reused. Profile invalidation keeps version-aware redacted record hashes.

Fresh EXTEND enters the montage panel; old saved styles and CREATE retain their editor.
Content changes rebuild/redistribute automatically and clear only current analysis,
while immutable previous revisions and billing evidence survive. Text editing/preview/
Save/reopen remain free. One analysis confirmation precedes contract2 submission;
received-result local recovery never resends. Trial preparation requires a saved current
analysis and freshly validated source/member bytes, then separate original verification
and generation consent. Random up-to3 eligible missing-app suggestions are saved using
existing ProjectSelection; protected/covered apps cannot enter default suggestions.
Back from Style resets to the source-summary logical step, avoiding a Targets/Style loop.
