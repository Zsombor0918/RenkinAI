# RenkinAI roadmap

Statuses describe implementation, not release approval. The application baseline
is [CURRENT_STATE](agent/CURRENT_STATE.md); test evidence is [BASELINE](qa/BASELINE.md).

| Phase | Status on October 12, 2026 | Exit condition / remaining work |
| --- | --- | --- |
| Upstream Renkin pipeline | Exists in source | Preserve editor, profile/storage, lock/provenance, backup/watch and APK exports |
| AI1: single-icon generation | Exists in source | Device/provider quality and Samsung compatibility still need recorded trials |
| AI2: selected missing batch + instructions | Exists in source | Device pause/cancel/process-death and pack-update scenarios remain open |
| Codex 00: docs, hooks, governance | Merged and owner accepted | [PR #1](https://github.com/Zsombor0918/RenkinAI/pull/1) merged as `005cffa6d51b11e15c20e68349c0c4ce0f23cd8f`; single-result P1 fixed and thread resolved. Actual current rule settings, including remaining review/conversation gaps, are in WORKFLOW |
| Codex 01: profile-backed ProjectHome | Merged and owner accepted; device acceptance pending | Real profiles, Create/Complete classic entry, safe asynchronous operations, Back/intent priority remain. The Robolectric dialog-width blocker is corrected in the test harness; complete evidence head passed 631 unit tests and debug APK. Owner merged PR #2 as `73a15e2210cb55cf003e60ebc2033130ee7a9e58` after final-head checks; physical devices NOT RUN; no automatic merge or release approval |
| Codex 02: separate Create/Extend and IconCoverageAnalyzer | Merged and owner accepted; device acceptance pending | Separate source/profile/target flow, bounded actual preview draw, independent local/lock state; new-pack targets use the full launcher inventory. M2 checkpoint a8b3e4b2 passed 669/669, all original/new isolated groups and debug APK. Owner authorized merge of PR #3 at head e8e04c43 as aefeb1b6; final M2 checks passed, device checks NOT RUN |
| Codex 03: project style/asset persistence | Merged and owner accepted; device acceptance pending | PR #4 source a73c1e02 passed 718/718, original/new groups, APK and three gates; owner merged as 6aed9e08 at 04:45:50 UTC. No device/provider/release approval inferred |
| Codex 04: references and reviewed style analysis | Implemented and offline-verified; owner merge/device acceptance pending | PR #5 code 47503e5c passed 792/792, all 16 retained/new groups, real APK and genuine v20 export/migration. Same-profile mode/source and 503 HTTP replay guards passed. Final-head gates belong in PR audit; no live/provider/device approval inferred |
| Codex 05–07: trial icons, staging gallery, AI edit | Planned | Separate product/quality/billing acceptance; current user-entered provider keys and AI1/AI2 remain |
| Hosted backend and public billing | Planned | Authentication, server spend controls and explicit product/security design; no backend in Codex 00 |
| Google Play release | Blocked pending evidence | Complete [release gate](policy/PLAY_RELEASE_GATE.md); no automatic approval from CI |

Do not mark a phase done merely because code exists or CI is green. Record the
change in ROADMAP when a milestone changes, its actual acceptance evidence in QA,
and its implementation rationale in the PR change note.
