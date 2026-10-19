# ADR0015 - retire obsolete active analysis workflows

Date: October18,2026. Status: owner-authorized Phase0 checkpoint; review pending.

## Context

The owner replaces Legacy/Quick/Standard analysis with a future montage-based
EXTEND workflow. The explicit Phase0–3 instruction authorizes deleting exclusive
production implementations and obsolete tests after dependency inspection. This
supersedes ADR0012's preservation of every old feature-specific test identity,
not its lean scheduling or safety/data requirements. Work stays on existing PR10.

## Decision

Remove active old analysis transports, coordinators, service/controller wiring,
analysis UI, runtime synthesis and outbound response-schema builders. Retain
historical codecs, fingerprint/prompt reconstruction, reviewed evidence and
journals/epochs/global consumed IDs required by persisted records and backups.
Do not migrate or rewrite stored data. Saved DNA-backed Trial inputs still use
their saved selected family/references; no active family/type analysis editor.

The existing free manual/reference style editor and Trial pipeline remain usable
as an independently testable fallback. No replacement paid operation is added.
Phase1–3 montage implementation is separate work within the same authorized
effort; this checkpoint does not claim that final product flow exists.

Delete obsolete feature-only tests; convert mixed fixtures to direct historical
data setup while retaining actual persistence and HTTP/image boundary assertions.
The exact old/replacement identities and dependency classification are recorded in
[the retirement inventory](../qa/PHASE0_RETIREMENT.md). No failing safety test is
waived. Remove selectors only when their entire production feature was retired.

## Verification scheduling

ADR0012 remains authoritative: one automatic Linux full-suite/build invocation,
HTTP included there, separate strict Python/governance and Android. The existing
Android harness now executes the active free reference Save, explicit Trial decline
and an independent-process restart, with exact2+1 counts. Physical-device guards,
source/head evidence and zero unauthorized submissions remain mandatory. UI order
diagnostics remain explicit and required for this UI-fixture change.

## Consequences and rollback

Old analysis creation is no longer available. Historical style/reference bytes,
full/shared backups, immutable TrialV1/V2 requests, uncertain/consumed request
identities and generated/approved variants remain readable. AI1/AI2, Global Batch,
editor/mapping/build/export, Room migrations and signing are unchanged. A normal
reviewed revert can restore UI/services without data conversion; never delete
journals, reset data or replay old requests. Any future montage contract must use
a new immutable generation version rather than edit V1/V2 in place.
