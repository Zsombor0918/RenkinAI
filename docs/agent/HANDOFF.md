# Agent handoff — Codex 03 project AI persistence

Base/main: `aefeb1b69e7757c757c60ac2f00ce2125893e1e6`. Branch:
`feat/project-ai-persistence-m3`, created cleanly from freshly fetched accepted main;
hooks installed before code changes. PR #3 is actually merged at 20:21:28 UTC on
October 11, owner Zsombor0918, head `e8e04c433ca94a4e28b5716f33581aeeb963b0a4`.
The merge has identical tree and contains the expected M2/governance files. There
were no open PRs/working-tree changes at the gate. Owner authorization supplied
acceptance; no independent approved review is claimed. Codex 03 is not merged.
Exact final feature SHA/PR/checks belong in its audit after commits.

## Implemented scope

Per-profile style text/instruction, ordered 0–5 private PNG references and explicit
local Save through existing setup/ProjectShell; unset starts, global copy only by
user action, no global AI1/AI2 change. Profile-keyed drafts survive recreation and
external editor ingress; Back Cancel/explicit discard protects them. Existing icon
Save/Cancel/session/launcher/Back ownership is retained. Room v19 adds profile-owned
metadata, immutable history, asset links and functional variant persistence; no
new DB or project identity. Current/source/candidate metadata is preparation, not
approval or paid permission. Source/catalog/user uncertainty requires fresh review.

Full backup and profile-share format 4 embed verified PNGs/history. Shares rekey
owners/paths; old full archives clear new metadata. Staging validates one bounded
input before writes; project files precede the final profile/icon/project transaction.
Legacy multi-store restore remains non-atomic; see the exact boundary in the ADR.
No credentials, batch journals or draft markers are exported. Existing signing and
pack identity remain. The real future gateway reuses existing clients with offline
HTTP seams; legacy AI1/AI2 still use their existing service (not full integration).

Files/rationale: [change note](../changes/2026-10-11-project-ai-persistence.md),
[ADR 0004](../decisions/0004-project-ai-persistence.md), [architecture](../ARCHITECTURE.md).

## Verification checkpoint and open risks

Local governance baseline: **45/45 passed**, 17.344 s. `git diff --check` and runner
Python compile passed before publication. Local `./gradlew compileDebugKotlin
 testDebugUnitTest assembleDebug` stopped at downloading Gradle 8.11.1 with Network
is unreachable; no local Android success. Fresh feature-head hosted checks are
still required; PR audit must stay BLOCKED until actual Android evidence exists.
New selectors retain every M1/M2 case, add project data/UI/gateway groups and retain
the real KSP schema export. Historical accepted M2 e8e04c43 passed 669/669 and APK;
that is not a Codex 03 result. Previous M1/M2 failures remain recorded in QA/change notes.

Devices/One UI/Theme Park/Flip/rotation/TalkBack, real URI providers, physical upgrade,
backup restore and pack update/install are NOT RUN. No live or paid API request.
Limits and explicit cleanup/partial legacy restore are documented; old APKs cannot
open v19. No 04–07 features or user-handle identity migration implemented.

Main ruleset 24877351 reread: active, no bypass, PR and exact build/commit-conventions/
documentation-audit required, force/delete forbidden. Approvals=0, conversation
resolution/last-push approval/strict checks=false; owner may strengthen these using
[WORKFLOW](WORKFLOW.md). Settings were not changed and are not claimed enabled.

## Next safe task

First finish feature-head migration/asset/backup/UI/gateway regressions, complete
unfiltered Android unit suite/APK and all governance checks; inspect final SHA and
PR review threads, then obtain owner/reviewer decision. Do not auto-merge or publish.
Rollback is a forward repair on v19 or explicit pre-upgrade-backup recovery, never
blind downgrade over new data. Keep application and pack signing continuity.

After explicit Codex 03 acceptance/main merge, Codex 04 may read current StyleRevision,
project instruction and ordered private references; revalidate saved Create/Extend
source/catalog selections first. Add a separate multimodal structured-text analysis
port/approval rules; this image gateway is not style recognition. Codex 05 trial icons
and approval, 06 candidate staging and 07 edits remain separate. No automatic source
sampler, billable GenerationPlan, billing consent, backend or credits exist now.


Local pre-publication verification: the staged documentation guard passed 43 changed paths. The full governance suite passed again, 45/45 in 18.348 s. No Android execution succeeded locally.

## First hosted Codex 03 correction checkpoint

Draft [PR #4](https://github.com/Zsombor0918/RenkinAI/pull/4), initial head
`ea5f4032228c2aab5ee9c954e722bb10020a6962`: Android compiled but 710/710 ran with
eight failures, zero errors/skips; APK skipped. Run 38174665908 and regression
38174665913 agree. Original form/Create/Extend/coverage and gateway+legacy groups
passed separately. This is preserved failure evidence, not a fresh success.
See [targeted corrections](../changes/2026-10-11-project-persistence-ci-corrections.md).
Actual KSP schema 19 is copied unchanged from artifact 11715826463.
Corrections fix DAO/thread/transaction and test synchronization/graphics semantics,
share the asset lease with restore/GC, and reread launcher/source context safely.
Fresh Android suites/APK are the next gate; audit remains BLOCKED and PR draft.
Governance passed again 45/45 in 24.813 s. No main/ruleset/live API changes.

Codex 03 c06d225a follow-up: compile PASS; 716 tests, three failures; APK SKIPPED
([Android 38175713388](https://github.com/Zsombor0918/RenkinAI/actions/runs/38175713388),
artifact 11715578079). Import-error cleanup must be candidate-scoped, not global
GC; the unchanged source-only M2 observer contract must stay default, while
launcher preparation opts into all package events only on its screen. Those two
corrections and opt-in/disposal coverage are now added. The one remaining real
selection Save interaction is being diagnosed with enabled/displayed assertions
and semantics/callback evidence; no claim of follow-up Android success yet.
See [cleanup correction record](../changes/2026-10-11-project-cleanup-regression-boundaries.md).
