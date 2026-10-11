# RenkinAI roadmap

Statuses describe implementation, not release approval. The application baseline
is [CURRENT_STATE](agent/CURRENT_STATE.md); test evidence is [BASELINE](qa/BASELINE.md).

| Phase | Status on October 11, 2026 | Exit condition / remaining work |
| --- | --- | --- |
| Upstream Renkin pipeline | Exists in source | Preserve editor, profile/storage, lock/provenance, backup/watch and APK exports |
| AI1: single-icon generation | Exists in source | Device/provider quality and Samsung compatibility still need recorded trials |
| AI2: selected missing batch + instructions | Exists in source | Device pause/cancel/process-death and pack-update scenarios remain open |
| Codex 00: docs, hooks, governance | Implemented on PR branch, review pending | Guard tests and PR CI recorded; owner review and external main protection required before closure |
| Codex 01: profile-backed ProjectHome | Not implemented | Separate authorized task: design against ProfileManager; preserve existing data and profile lifecycle |
| New AI UX/capability improvements | Planned | Separate scope and tests after ProjectHome; current user-entered keys remain during testing |
| Hosted backend and public billing | Planned | Authentication, server spend controls and explicit product/security design; no backend in Codex 00 |
| Google Play release | Blocked pending evidence | Complete [release gate](policy/PLAY_RELEASE_GATE.md); no automatic approval from CI |

Do not mark a phase done merely because code exists or CI is green. Record the
change in ROADMAP when a milestone changes, its actual acceptance evidence in QA,
and its implementation rationale in the PR change note.
