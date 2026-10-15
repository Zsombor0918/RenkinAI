# Documentation index

Read [AGENTS.md](../AGENTS.md) first, then CURRENT_STATE, ROADMAP and HANDOFF.

Current PR8 reliability/fidelity follow-up: [report contract and diagnostics](changes/2026-10-15-dna-report-contract.md),
[derived overall guidance and matching references](changes/2026-10-15-dna-overall-guidance.md),
and [no-cost visual acceptance rubric](qa/STYLE_DNA_FIDELITY.md). Exact final source,
all checks and verified fresh APK evidence belong directly on draft PR8 after execution.

Latest real-device report and narrow offline correction:
[first-submission Responses envelope](changes/2026-10-15-dna-response-envelope.md).

Latest PR8 provider-result correction: [reproduction, state/event table and narrow fix](changes/2026-10-15-dna-provider-result-correction.md).
Retained-controller recreation loses a frozen plan on the starting source; old
failure prose also conflates envelope/reports/synthesis. Final-head evidence and
exact APK link/hash belong in the draft PR audit after execution.

PR8 final verification source: [observed974-test success and closing gates](changes/2026-10-15-dna-final-verification.md).
Its own final SHA/checks/APK are recorded externally on draft PR8 after execution.

Latest PR8 end-to-end follow-up: [current frozen plan, history and refresh race](changes/2026-10-15-dna-plan-refresh.md).
Exact final-head verification and APK evidence are recorded on draft PR8.

Latest PR8 owner finding: [cross-pack local storage and diagnostics](changes/2026-10-15-dna-shared-storage.md).
Exact final-source checks and downloadable APK evidence are in the draft PR audit.

Current local guided UX Stage0/1: [evidence audit](changes/2026-10-14-guided-ux-audit.md),
[implementation and checks](changes/2026-10-14-guided-ux-implementation.md),
[closing readiness correction](changes/2026-10-15-guided-ux-stabilization.md),
[ownership ADR0009](decisions/0009-guided-project-workflow.md) and
[Flip8 owner check / Stage2+3 plans](qa/GUIDED_UX_OWNER_CHECK.md).
The branch is `feat/ux-flow-stabilization`, stacked on verified PR7 `2f422ca8`;
PR6/PR7 remain draft/unmerged. Older checkpoint prose below is historical evidence.

Current Style DNA scope, complete changed-file inventory and genuinely executed red/green
evidence: [October14 verification](changes/2026-10-14-style-dna-verification.md).
Current facts come from the checked-out source; inherited prose is not a substitute.
The Codex 00 baseline is `main` at `eb4daa5de82fab54e115363c073eb8981f2203d6`
(verified October 11, 2026). Commit-specific validation is recorded separately.
Codex 00–04 are owner-merged. PR #5 was merged as af7bdb10 at 16:10:49 UTC
on October 12; its final aceb691c checks passed independently. Codex 05 trial
is implemented in [PR #6](https://github.com/Zsombor0918/RenkinAI/pull/6), with
verified style-fidelity/component-original checkpoint 2456bf5e/898 tests and all21
regression groups; the closing commit's own final-head checks are in the PR audit.
Owner confirms analysis and single-icon trials work; the new style/original refinement
still needs owner device acceptance in the same draft/open PR.
The [verification record](changes/2026-10-13-style-fidelity-verification.md) records
the new red-to-green assertions, bounded benchmark and remaining phone gates.
The [test-first proof](changes/2026-10-13-single-icon-trial-reproduction.md) and
[OpenAI contract correction](changes/2026-10-13-single-icon-trial-contract.md) retain
legacy/Stability bytes and old results; exact final automated evidence is in PR#6.
The [remaining CI repair](changes/2026-10-13-style-trial-ci-repair.md) records the
source-free CREATE correction and preserved routed assertions from baseline 3133b522.
Owner review/merge is pending. Codex 06/07 remain planned; device/provider acceptance is separate.

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
| [Trial consent/recovery ADR](decisions/0006-style-trial-consent-and-recovery.md) | Separate trial UUIDs, durable RAW/review/approval, v21 migration and M6 read-only reuse |
| [Style fidelity/originals ADR](decisions/0007-style-fidelity-and-component-originals.md) | Bounded local geometry/palette evidence, compatible journals and explicit original verification |
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

Style DNA is opt-in stacked development in [PR7](https://github.com/Zsombor0918/RenkinAI/pull/7), based on PR6 head454bd5ec. [ADR0008](decisions/0008-style-dna-stacked-development.md) records its bounded report/grammar/request decisions. Old M4/M5 remains available; final-head evidence and owner visual acceptance are required.
