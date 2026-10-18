# RenkinAI roadmap

Owner-approved Phase3A is separate stacked feature work from completed PR8
source38311206: local source references, reviewed brief, free Save and existing
Trial preparation, with optional historical analysis preserved. Implementation
and final-head verification are recorded on its own draft PR; this is not owner
release approval. Phase4 prompt simplification and paid experiments remain deferred.
See [reference-first decision](decisions/0013-reference-first-style.md).


Statuses describe implementation, not release approval. The application baseline
is [CURRENT_STATE](agent/CURRENT_STATE.md); test evidence is [BASELINE](qa/BASELINE.md).

| Phase | Status on October 13, 2026 | Exit condition / remaining work |
| --- | --- | --- |
| Upstream Renkin pipeline | Exists in source | Preserve editor, profile/storage, lock/provenance, backup/watch and APK exports |
| AI1: single-icon generation | Exists in source | Device/provider quality and Samsung compatibility still need recorded trials |
| AI2: selected missing batch + instructions | Exists in source | Device pause/cancel/process-death and pack-update scenarios remain open |
| Codex 00: docs, hooks, governance | Merged and owner accepted | [PR #1](https://github.com/Zsombor0918/RenkinAI/pull/1) merged as `005cffa6d51b11e15c20e68349c0c4ce0f23cd8f`; single-result P1 fixed and thread resolved. Actual current rule settings, including remaining review/conversation gaps, are in WORKFLOW |
| Codex 01: profile-backed ProjectHome | Merged and owner accepted; device acceptance pending | Real profiles, Create/Complete classic entry, safe asynchronous operations, Back/intent priority remain. The Robolectric dialog-width blocker is corrected in the test harness; complete evidence head passed 631 unit tests and debug APK. Owner merged PR #2 as `73a15e2210cb55cf003e60ebc2033130ee7a9e58` after final-head checks; physical devices NOT RUN; no automatic merge or release approval |
| Codex 02: separate Create/Extend and IconCoverageAnalyzer | Merged and owner accepted; device acceptance pending | Separate source/profile/target flow, bounded actual preview draw, independent local/lock state; new-pack targets use the full launcher inventory. M2 checkpoint a8b3e4b2 passed 669/669, all original/new isolated groups and debug APK. Owner authorized merge of PR #3 at head e8e04c43 as aefeb1b6; final M2 checks passed, device checks NOT RUN |
| Codex 03: project style/asset persistence | Merged and owner accepted; device acceptance pending | PR #4 source a73c1e02 passed 718/718, original/new groups, APK and three gates; owner merged as 6aed9e08 at 04:45:50 UTC. No device/provider/release approval inferred |
| Codex 04: references and reviewed style analysis | Owner-merged; device/provider acceptance pending | PR #5 code 47503e5c passed 792/792, all 16 retained/new groups, real APK and genuine v20 export/migration. Same-profile mode/source and 503 HTTP replay guards passed. Final aceb691c passed its own complete checks; owner merged #5 as af7bdb10 at 16:10:49 UTC; no provider/device approval inferred |
| Codex 05: style trials and approval | Implemented; owner visual acceptance/review/merge pending | Owner confirms analysis/trials run after the collage correction; PR#6 now refines representative local sampling, color philosophy and component-original verification. Shared OpenAI single-target/one-output contract restores image roles without changing legacy/Stability bytes or saved PNGs. Require corrected Flip8 one-icon acceptance after separate new paid consent; exact final-head Android/all21/governance/XML/APK evidence is in PR#6. M6/M7 stay planned |
| Style DNA D0–D3 | Code verified925/925 and all22 groups on stacked draft PR7; closing-head evidence in PR audit | Per-image reports, multi-family reviewed grammar and target recipes; Deep/live D4 and owner visual acceptance deferred |
| Guided UX Stage0/1 | Implemented on local stacked branch; owner review/device acceptance pending | Four steps, awaited saves, current target hydration and truthful review/completion; final-head local gates/APK and draft audit separate from phone/visual acceptance |
| Guided UX Stage2: fidelity | Plan only | Fixed three originals, Legacy/Quick/Standard blind comparison after separate paid consent and current pricing/spend proposal |
| Guided UX Stage3: cleanup | Plan only | Caller-proven small follow-ups; preserve historical failure notes, ADRs and live legacy/editor paths |
| Codex 06: staging gallery / bulk Apply | Planned | Read-only reuse of accepted identical M5 inputs; new paid batch consent and explicit Apply remain separate |
| Codex 07: AI image edit / grouped editing | Planned | Original editor remains available; no new image edit or bulk modifier in M5 |
| Hosted backend and public billing | Planned | Authentication, server spend controls and explicit product/security design; no backend in Codex 00 |
| Google Play release | Blocked pending evidence | Complete [release gate](policy/PLAY_RELEASE_GATE.md); no automatic approval from CI |

Do not mark a phase done merely because code exists or CI is green. Record the
change in ROADMAP when a milestone changes, its actual acceptance evidence in QA,
and its implementation rationale in the PR change note.
