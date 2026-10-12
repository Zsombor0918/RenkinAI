# Documentation index

Read [AGENTS.md](../AGENTS.md) first, then CURRENT_STATE, ROADMAP and HANDOFF.
Current facts come from the checked-out source; inherited prose is not a substitute.
The Codex 00 baseline is `main` at `eb4daa5de82fab54e115363c073eb8981f2203d6`
(verified October 11, 2026). Commit-specific validation is recorded separately.
Codex 00–03 are owner-merged. Codex 04 code is offline-verified in open PR #5;
CURRENT_STATE and HANDOFF distinguish its 792-test checkpoint, exact final PR-head
gates and pending owner/device/provider acceptance. Codex 05–07 remain planned.

| Document | Responsibility |
| --- | --- |
| [README](../README.md) | Product overview, upstream attribution and entry points |
| [CONTRIBUTING](../CONTRIBUTING.md) | Setup, Windows/Git Bash, commits, hooks and contributor commands |
| [CHANGELOG](../CHANGELOG.md) | User-visible release changes |
| [CURRENT_STATE](agent/CURRENT_STATE.md) | Source-backed implementation and known limitations |
| [ARCHITECTURE](ARCHITECTURE.md) | Existing layers, persistence and integration boundaries |
| [ROADMAP](ROADMAP.md) | Milestones and their actual implementation status |
| [WORKFLOW](agent/WORKFLOW.md) | Agent/PR lifecycle, automatic rules and main protection |
| [HANDOFF](agent/HANDOFF.md) | Last closed work, current risks and next safe task |
| [Changes](changes/README.md) | New detailed record for each meaningful PR |
| [Governance ADR](decisions/0001-repository-governance.md) | Durable decision, alternatives and consequences |
| [Project AI persistence ADR](decisions/0004-project-ai-persistence.md) | Profile-owned immutable history/assets, migration, full/share backup and gateway limits |
| [Reviewed analysis ADR](decisions/0005-reviewed-style-analysis.md) | Bounded references, consent-bound recovery, structured evidence and explicit style acceptance |
| [Read-only coverage ADR](decisions/0003-read-only-source-coverage.md) | Source facts separate from local profile state, bounded preview and user-identity limits |
| [Project/session ADR](decisions/0002-profile-backed-project-home.md) | Profile IDs as projects, single session and explicit asynchronous navigation |
| [BASELINE](qa/BASELINE.md) | Fresh verification versus historical results |
| [TEST_MATRIX](qa/TEST_MATRIX.md) | Regression coverage and outstanding device scenarios |
| [PLAY_RELEASE_GATE](policy/PLAY_RELEASE_GATE.md) | Evidence required before a future public release |
| [AI_FORK_HU](AI_FORK_HU.md) | Hungarian usage, build and AI/storage constraints |
| [AI_HANDOFF_HU](AI_HANDOFF_HU.md) | Preserved October 4 handoff; historical evidence only |
| [Icon-watch design](icon-watch-feature.md) | Preserved historical rationale; superseded by current source |

Only one canonical architecture and current-state document exist. User guides may
refer to them. Do not create another parallel architecture or silently promote a
historical test result into the current baseline.
