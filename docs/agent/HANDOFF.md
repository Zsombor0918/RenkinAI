# Agent handoff — Codex 03 project AI persistence

Accepted base/main: `aefeb1b69e7757c757c60ac2f00ce2125893e1e6`. Owner-authorized
PR #3 merge at 20:21:28 UTC on October 11, 2026; head e8e04c43 is an ancestor with
identical tree. Codex 03 branch `feat/project-ai-persistence-m3`,
[PR #4](https://github.com/Zsombor0918/RenkinAI/pull/4); no Codex 03 merge/release.
The exact final SHA/checks belong in the PR audit after commits. Hooks installed;
main/ruleset and historical commits were not changed by this feature.

## Implemented scope and ownership

Existing Profile.id and existing Room DB v19 own metadata, immutable StyleRevision
history, ordered 0–5 private PNGs and functional variant records. Create/Extend
setup opens local style/instruction/import/reorder/Save; unset starts and global
copy is explicit. Existing session mutex and expected revision/version protect
wrong-profile/stale writes. Only durable completion reports Saved. Primitive,
profile-keyed drafts retain Back/Cancel/recreation/ingress behavior. No second VM,
provider, database, generation/approval or icon autosave is introduced.

Private assets use bounded PNG/CRC/hash checks and temp/fsync/rename before Room.
IO/per-DB asset lease serializes restore/import/explicit GC. Candidate-only failed
import cleanup retains earlier pending/history/variant/unrelated files; explicit
remove/discard covers saved draft-import hashes. Fresh launcher/source verification
requires current selectable keys; new/unloaded/work-user uncertainty is disabled.
Source-only observation remains default, preparation opts into screen-only launcher
changes, disposal unregisters. Save does not refresh icons or alter primary source.

Format 4 full backups and profile shares carry verified PNG/history/variants; shares
allocate/remap existing profile IDs. Old full archives clear new metadata. Stage and
validate one bounded immutable ZIP before writes; files precede final profile/icon/
project transaction. Legacy watch/prefs/presets/gallery/keystore remain separate and
can be interrupted partially. Gallery compatibility allows genuine transparent or
larger sampled PNGs with a 20 MiB bound; project refs retain 4 MiB/nonempty rules.
No credentials, batch journals or draft markers travel. v19 rollback is forward
repair or explicit pre-upgrade-backup recovery; v18 APK cannot read populated v19.

Real future IconGenerationGateway reuses existing clients/AiCredentialStore. Offline
MockWebServer/fakes verify order/0–5 refs, Stability exactly two, invalid/timeout/
cancel/429 no retries. Legacy AI1/AI2 still use their existing global service, not
this gateway. No live/paid request, backend, billing or Codex 04–07 implementation.

## Verified checkpoints and next gate

- Initial ea5f4032: compiled, 710 tests/eight failures; APK skipped.
- c06d225a: compiled, 716 tests/three failures; APK skipped.
- 02c23aa5: original CI attempt had 490 pre-test Maven fixture checksum 404s;
  same-head retry and independent regressions narrowed to one failure in 717.
- 88933c95: all filtered groups and 717/717 full tests, zero failure/error/skip;
  compile/debug APK PASS in Android 38176787953 and regressions 38176787903,
  artifact 11716765047. The measured old fake version comparison was false at
  initial version zero; explicitly typed Long produced one save and correct keys.
  This was a test backend check, not a production Save/layout/idling failure.
- 06e71532: legacy-gallery compatibility verified. Android 38177165898 and
  regressions 38177165977 PASS including debug APK. Artifact 11716575779 has
  127 full-suite XML, independently counted 718/718, zero failure/error/skip;
  original cases 1/1, 1/1, 2/2, 3/3, Create/Extend 10/10, coverage 28/28,
  UI 12/12, data/session 31/31, gateway/legacy 31/31 all pass. Format-3 larger
  and transparent gallery restore compares exact PNG bytes; project references
  still reject them. APK artifact 11716760691 exists at that same SHA.
- Real KSP v19 export is committed unchanged: identity 7e4b1aec61364b15b862957ac574c8b7.
  Migration actual-v18 fixture, reopen/current identity/FK/legacy data passed.
- Local guards 45/45 (latest 16.053 s), staged/range docs and conventional hooks PASS.
  Local full Gradle command failed before configuration: Network is unreachable.

This last commit removes temporary diagnostics without weakening any assertion
and consolidates canonical docs. Its own complete isolated/full Android/APK/
governance results and exact final SHA are recorded in PR #4 after publication.
Only all-green final checks permit a PASS WITH RISKS audit and review-ready state;
owner acceptance is still required. Earlier-head green is not final-head proof.
Failed history remains in BASELINE/change notes.
ADR 0004 now points to the verified legacy-gallery checkpoint rather than its
previous pending sentence. This documentation consistency follow-up adds no
runtime or test changes; its own final contexts still belong in PR #4.
Physical devices/URI providers/upgrade/SAF/signing/installer/TalkBack/OEM NOT RUN.

## Main rules and owner acceptance

Ruleset 24877351: active, main-only, empty bypasses, PR plus build/commit-conventions/
documentation-audit, force/delete prohibited. Approvals=0; conversation resolution,
last-push approval and strict up-to-date checks=false. Do not claim these owner
settings enabled. See WORKFLOW. Owner/reviewer acceptance remains mandatory even
with green CI; do not merge/publish or change rules without separate authorization.

## Next safe development boundary

First finish the final evidence and review of PR #4. After explicit Codex 03
acceptance/main merge, Codex 04 may read current revision specification, project
instruction and ordered private validated refs, with fresh Create/Extend source/
launcher review. Add its own multimodal structured-text analysis port and approval
contract. The image gateway is not analysis. Automatic reference sampling, 05
trials/approval, 06 staging, 07 edits and a billable GenerationPlan remain separate.
Global legacy AiSettingsStore remains the AI1/AI2 default; project Save grants no
cost permission. Preserve signing, package identity and manual editor/backup paths.

See [initial implementation](../changes/2026-10-11-project-ai-persistence.md),
[ADR](../decisions/0004-project-ai-persistence.md), [architecture](../ARCHITECTURE.md)
and [QA evidence](../qa/BASELINE.md).
