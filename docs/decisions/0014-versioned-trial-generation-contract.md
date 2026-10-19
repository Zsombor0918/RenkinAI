# ADR0014 - freeze a reference-first Trial generation contract

Date: October18,2026. Status: owner-authorized Phase4A, review pending.

## Context

Reference-first Save needs no analysis. The historical DNA Trial concatenates a
target recipe and a saved specification even when both compile the same rules.
The V1 contract also emphasizes retaining logo proportions rather than explicitly
allowing stylistic reconstruction. Existing payable plans must retain their text.

## Decision

Only newly prepared OpenAI Trial plans use generation contract V2. Missing version
means V1; its canonical fields, fingerprint and prompt bytes remain unchanged.
V2 adds optional promptVersion and per-target promptHashes to the same schema1
plan. Each hash binds the full UTF-8 provider prompt, including ordered-image role
count, to the plan fingerprint. Parse/validation recomputes that exact version and
rejects mismatches before claim/submission. V2 text is immutable; future changes
require another version. Stored historical requests are never upgraded or replayed.
Hashes avoid duplicating up to three full prompts inside bounded journal records.

V2 has one authoritative identity/image/output contract and one quoted appearance
brief. Human-reviewed style/project/app guidance resolves style ambiguity; visual
references supply construction; supplemental old drawing guidance is subordinate.
Only recognized historical compiler envelopes lose provenance counters, family
headers and unestablished-trait lists. Exact whitespace-normalized duplicate lines
are omitted across the two inputs; unique reviewed or target guidance is retained.
Ordinary manual prose is not heuristically summarized or semantically guessed.

Original identity is distinct from permitted geometric redrawing. Dark silhouettes
do not establish tiles; transparent canvas does not prohibit evidenced tile art.
Meaningful brand colors are default, with reviewed fixed/mono/duotone exceptions.
One standalone1024x1024 transparent PNG remains n=1/background=transparent/png.
No model/provider, legacy null-override AI1/AI2, Stability or analysis change.

## Safety, verification and rollback

Existing journal claim, exact consent, endpoint markers, current-input validation,
original verification and no-replay ledger remain authoritative. Redacted deleted
V2 records receive hashes for their redacted text only; consumed IDs remain.
No Room/backup migration or new store. Old apps cannot read new optional fields;
rollback must retain a compatible reader, not erase or resubmit those journals.
ADR0012 requires focused tests then one final-head Linux suite/build, Python and
the existing five Android journeys. Exact results belong in the external PR audit.
Semantic/HTTP checks do not establish visual fidelity; no live request is allowed.

## Alternatives

Changing V1 silently breaks frozen payable evidence. Deleting every specification
loses reviewed edits. A new analysis schema does not solve request composition.
Full duplicate prompt strings inflate bounded journals without improving identity
binding. Broader analysis retirement and paid visual pilots remain separate phases.
