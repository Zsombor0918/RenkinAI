# ADR0016 - use one persisted pack montage for visual style

Date: October18,2026. Status: owner-authorized Phases1–3; local foundation implemented, UI integration pending.

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
