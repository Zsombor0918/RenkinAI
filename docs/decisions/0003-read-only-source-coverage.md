# ADR 0003 — separate source coverage from profile preparation

Date: October 11, 2026. Status: proposed for owner review in Codex 02.

## Context

A source pack declaration is not proof that a drawable exists or draws. The quick
AI2 missing filter describes the profile, not external source coverage. Profile IDs
and the existing Activity VM are already the authoritative project/session owners.
Launcher component keys do not encode Android user identity.

## Decision

Use an independent, read-only analyzer and a narrow reader on the existing pack
repository. Capture index quality during the same existing XML parser pass; preserve
legacy RawElements and last-valid duplicate selection. A sole malformed item is
still declared broken. Require a bounded, nontransparent static preview draw for
RESOLVABLE; preserve unknown/unavailable instead of recommending generation.

Keep local manual/applied/fallback/lock state orthogonal. Only unmapped or broken
entries with no local/fallback icon and safe unlocked identity are recommended;
explicit overrides remain user selections, never API or quick-batch input.

CREATE uses all known launcher entries without a source requirement. EXTEND chooses
an external package and then an explicit existing/new profile using the accepted
ProjectSessionController Save/Cancel contract. Store only primitive UI selection
and source context; no Room or style/asset persistence in this milestone.

Preserve all classic editor sources and build paths. Inspection never changes
primary source preferences or refreshes icons. A separate explicit classic source
selection retains its existing behavior. Resume/package/version/index/theme and
component/local changes invalidate reads; no cross-scan bitmap cache.

## Alternatives

- Reuse quick-batch `createdIcon == null`: ignores actual external mapping/draw
  and suppresses Create inventory; rejected.
- Count `matchedAppCounts` or resource IDs: certifies broken resources; rejected.
- New XML parser/catalog/project store: duplicates identity and existing semantics;
  rejected. Small callbacks in the existing parser preserve legacy behavior.
- Add user handles to Room keys now: requires migration/backup/signing compatibility
  work outside Codex 02. Record uncertainty before existing deduplication and disable
  non-current-user/collision rows; a separate decision must precede migration.
- Persist selection/style metadata now: belongs to Codex 03; deferred.

## Consequences

Sequential 96 px software draws run on Default, discard previews and cache only
Boolean outcomes within a scan. Cancellation is cooperative between targets;
pathological synchronous resource draw can delay a cancellation. Static previews
cannot certify animated clock/calendar, full themes/monochrome or OEM launcher output.
No schema, provider endpoint, signing, package or permission change. Real-device
coverage/install/rotation remains a separate acceptance requirement. See
[architecture](../ARCHITECTURE.md), [state](../agent/CURRENT_STATE.md) and
[change note](../changes/2026-10-11-create-extend-coverage.md).
