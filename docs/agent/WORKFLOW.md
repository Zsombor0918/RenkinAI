# Agent, Git and review workflow

## V4 montage acceptance evidence

The existing Android validator now requires exact prompt_version4 for the new montage
journey; stale V3 and V2 receipts fail. Test count, separate-process restart, actual
loopback/external counters and all other gates remain unchanged. ADR0012 still gives
one full hosted suite/build; no additional workflow or automatic unit matrix is added.

## Montage evidence scheduling

ADR0012 remains unchanged: CI/build alone runs the authoritative full suite and HTTP. It additionally retains actual synthetic montage PNGs/metadata, without another unit invocation. Existing Android runner now executes3 write journeys plus1 separate-process restart; strict Python fake-runner assertions require those counts, input roles and zero external posts. Shared-navigation changes also require the existing dispatch-only7-case order check. Historical merge-title governance is not waived.

## Phase0 retirement verification

Owner authorization in ADR0015 allows only dependency-proven obsolete analysis cases/selectors to be removed. The single automatic full Linux/build and integrated HTTP policy remains; two empty retired analysis groups are removed, historical-data groups retained. Android now requires2 active write/preparation journeys plus1 separate-process restart, with unchanged emulator-only/head/counter guards. Run the explicit form-order diagnostic for these UI-fixture edits. Existing PR10 remains draft/open; its actual owner-selected base is main. No merge/rewrite/new PR. Phase0 evidence must not be presented as completion of montage Phases1–3.

## Phase4A stacked verification

Use the existing PR base feat/ux-flow-stabilization and separate draft feature branch. No CI scheduling change: ADR0012 single full Linux/build plus Python/governance and existing five Android journeys. Record exact final SHA externally after commit; initial BLOCKED audit is retained until evidence supports a final result. No shared Compose harness/order change is introduced by request-contract assertions.

## Phase3A stacked feature verification

CI, offline Android and governance add PR base feat/ux-flow-stabilization so the
separate reference-first draft PR receives the same gates. Scheduling remains
ADR0012: one automatic full unit suite/build, no duplicate HTTP job. Android adds
one local reference-first journey to the retained Quick/Standard/incomplete-cancel
and restart paths (4 writes/preparations plus1 independent restart process).
The evidence validator requires zero loopback/external POSTs for reference-first,
ordered saved references, Trial preparation and three saved projects reopened.
Only the explicit qemu-guarded emulator may be installed/stopped. No phone access.


## Owner-approved lean verification (October 17, 2026)

This policy supersedes earlier requirements below to execute every historical group
on every PR. It changes scheduling only; all Kotlin cases/assertions remain.
[ADR0012](../decisions/0012-lean-verification.md) records the owner authorization and exact execution inventory.
CI/build alone runs the full unit suite, compile and debug assembly through
`python scripts/run_compose_regressions.py --group full-suite`, retaining exact-head
XML/HTML/logs, required selectors and real Room19/20/21 exports. HTTP contracts are
included there, not in a second automatic job. Android instrumentation and strict
Python/governance checks remain separate; hosted Standard/restart remain BLOCKED.
For development use `--group trial-persistence-session` (or another existing name);
repeat `--group` to run a deduplicated union in one Gradle invocation. No argument
also selects only full-suite. `--order-check` explicitly runs creation, ingress,
both and shared-form separately; run it for shared UI harness/order changes and
before owner pre-merge/release review, via the dispatch-only Profile form workflow.
Record its exact SHA and result; a missing diagnostic is NOT RUN, never PASS.
Older results below remain historical, not final-head evidence. No safety/data,
billing, signing, API, test failure condition or owner-review requirement is waived.


## Synthetic emulator transition evidence

The offline runner additionally retains tag-filtered trace-0.log and trace-1.log from the explicit emulator after each instrumentation command. Test-owned delegates record actual input/backend boundaries, observed state and child completion, plus Save/read/journal events with monotonic thread/process identity. Logging never changes a dispatcher or grants success; absent transitions may reflect StateFlow conflation or watcher attachment. No raw credentials, image bytes, SQL arguments or provider prose are logged. Existing gates and strict counter/assertion checks remain.

## Offline DNA gates and Unicode Windows paths

The original23 regression groups remain; an additive offline HTTP group requires
the new disk-Room/controller/actual-wire suite and incomplete-reason regressions.
The runner preserves an explicit ASCII junction path so AGP's Windows path guard
can stay enabled. A new Offline Style DNA workflow runs failing HTTP and emulator
jobs on the actual PR head and retains reports. Its host runner refuses physical
serials/qemu mismatches, builds the three APKs, records hashes/source/dirty status,
and verifies separate instrumentation processes. It never reads a provider key.
These are workflow gates; no new branch-protection context is claimed configured.
The existing CI build also pins checkout to the PR head (or event SHA on push/
dispatch), retaining all tasks while making final APK provenance explicit.
See [offline commands](../qa/OFFLINE_STYLE_DNA.md) and
[ADR0010](../decisions/0010-offline-style-dna-validation.md).

## PR8 final APK verification

The existing CI compile/unit/assemble/upload sequence retains every task. After
assembly an additive SDK35 aapt/apksigner/sha256sum step verifies the debug package
hu.zsombor.renkinai.dev, nonempty APK, complete signature and certificate/hash.
The hosted6fd75af2 run proved rg unavailable after a successful974-test/assemble
run; package matching therefore uses standard grep with an anchored exact pattern.
Download that exact run's artifact and compare its APK SHA256 to this output.
This verifies the artifact, not installed-certificate compatibility or phone
acceptance. No signing configuration, dependency, required gate or protection rule
changes. Final exact-head all23/XML/schema and audit checks remain mandatory.

## Native Windows guided UX verification

The complete Compose runner now has 23 groups, retaining all 22 previous groups and
selectors and adding five guided classes plus the AtomicFile harness class. Windows invokes the absolute native
`gradlew.bat`; POSIX still invokes `./gradlew`. Task/selector/XML/skipped/error/exit
requirements are unchanged. The final full-suite group compiles, tests and assembles
on the exact committed HEAD after the isolated runs.

Git snapshot reads normalize CRLF before matching the same exact four impact
declarations. Strict declaration/body/path/link requirements remain; a retained test
proves native Windows blobs work. Hook fixture text explicitly uses LF matching
`.gitattributes`, so the real pre-commit whitespace gate is still exercised.
All three workflows additionally trigger for PRs targeting `feat/style-dna-v1`.
No required job or guard was removed and no server protection setting was changed.
See [ADR0009](../decisions/0009-guided-project-workflow.md).

Local setup is process-scoped via portable `env.ps1` (JDK17/API36/build-tools35.0.0,
Python3.14/Git shell on PATH). Run `python -m unittest discover -s scripts/tests -v`,
`python scripts/run_compose_regressions.py`, `python scripts/check_docs.py --base
origin/feat/style-dna-v1 --head HEAD`, and the commit range/PR audit guards.
Record final-head evidence externally; do not substitute earlier remote CI or a stale APK.
The stacked PR stays draft; PASS WITH RISKS still requires explicit owner review.

The Windows-only API30+ AtomicFile shadow preserves Android atomic replacement in
Robolectric; non-Windows invokes the original method. Four mandatory harness guards
and unchanged journal/controller guards verify base/backup/unfinished-write durability.
Changes to shared Java shadows, test resources and the harness-test package also trigger
the same mandatory isolated workflow, preserving coverage for harness-only corrections.
Its narrow8-test pass does not replace the unfiltered/all23 gates; see the
[platform correction](../changes/2026-10-14-windows-atomic-test-harness.md).

## Earlier checkpoint records

The dated evidence below is historical; it does not replace the local guided UX checks.

## Closing-head legacy rejection-journey readiness

Closing docs-only63e5e5be passed Android38478557077 and925/925 complete unfiltered
tests. Full regressions38478557064 failed1/23 isolated trial-ui assertions because
the retained rejection journey clicked style-analysis-prepare while its independent
initial Room load kept the real button disabled. All other21 commands, including
DNA26/26 and full925/925, passed. Independent artifact11856632498,644124bytes,
SHA25682ec516e2af3be8bdba3fa42687adaf7b69135a0f8b7635f2f5e4e4f40abbb5b
retains the actual failure; it is not erased by full-suite success.

The test-only follow-up materializes and observes the actual enabled semantics within
the existing10000ms bound in that second journey, exactly as the earlier EXTEND journey
already does. Every displayed/enabled/click, error, consumed-consent, fresh-ID, manual
Save and zero-image-call assertion remains unchanged. No runtime, selector, timeout,
idling rule or signing/data change. Its own final-head all22/Android/governance,
independent XML/schema and fresh APK are still required in PR7's audit.

## October 14 — Style DNA closing-head verification

Style DNA code89eacb0d passes original Android tasks and all22 runner groups:
149 XML/925 actual tests, zero failures/errors/skips, all selectors and genuine schema
exports present. Independent artifact11857125237 is638786 bytes, SHA256
`076d772e6961d67ef9a50652fe5cdfed117b2cf90451c4964fcff8eea9585f12`.
Initial b2df166a904/6 and corrected compilation4531da8a925/1 remain observed red evidence.
No selector, assertion, timeout, idling, failure condition or existing job was weakened.

For the closing docs-only HEAD run all mandatory local guards/range checks, complete
runner and Gradle attempt, then hosted Android/all22/commit-conventions/documentation-audit.
Local SDK/bootstrap network failure is not Android success. Independently download final-head
XML/schema evidence and APK, verify package/digests/signature and update PR7's audit with
the final SHA. BLOCKED stays until mandatory gates finish; PASS WITH RISKS may record
explicit device/live gaps without claiming owner review/merge. Keep stacked draft unmerged.

## October 13 owner reproduction coverage

The existing trial-ui group additionally includes ProjectStyleTrialJourneyComposeTest.
All21 groups and previous selectors remain. NativeSDK33/320dp tests traverse the
actual shell/targets/style/trial routes with real Room, sampler and owned controllers.
Only artwork, session ownership callbacks and provider ports are fixtures; this is
not a full Android MainViewModel/Hilt or phone test. The red checkpoint requires
recorded failing assertions before corrective changes are declared verified.


Start with [AGENTS.md](../../AGENTS.md); setup is in [CONTRIBUTING](../../CONTRIBUTING.md).
The shared scripts own the automatic rules. This document explains their limits
and the external settings required to turn checks into enforced merge conditions.

## One work item

Inspect current branch/HEAD/tree/PRs, read the index/state/roadmap/handoff, inspect
related source, define scope and preserve the Renkin integration paths. Work on a
topic branch. Implement, update canonical docs, add a dated change note, run tests,
update handoff, commit with a validated subject, and open an audited PR. Report the
base and final head SHA. Leave reviewer acceptance and merge to the authorized owner.

## Profile form regressions

The read-only PR workflow `compose-regressions.yml` runs
`python3 scripts/run_compose_regressions.py` on the exact PR head: both formerly
blocked real-form shell tests individually, together, then the shared-form class.
It additionally runs CreateExtendFlowComposeTest and the coverage domain/parser/render fixtures separately, without narrowing any original selector. It retains per-case XML/HTML/logs and `build/compose-regressions/summary.json` with
HEAD, exit codes and counts. QA-document changes also trigger this workflow so a final evidence commit receives its own complete regression run; prior-head green checks are not substituted. Nonzero Gradle exit, missing results, failure/error or
skipped tests fail the runner/job. The full Android CI still runs the entire suite
and debug build; filtered regressions do not replace it or the three required checks.
After the filtered runs the runner executes the unfiltered
`./gradlew compileDebugKotlin testDebugUnitTest assembleDebug` command, retains
complete suite XML/counts, and verifies that requested selectors occurred. The
additional complete run also checks order dependence after the filtered runs.

Native SDK 33 form tests use 320 dp width to avoid the measured Robolectric
WRAP_CONTENT custom-width loop. Wide/hinge Home tests remain unchanged; wider-device
form/TalkBack checks remain manual. No production dialog sizing, clock, timeout or
idling behavior was changed. Temporary probes/reflection, draw/window overrides
and known-negative timeout cases were removed after capturing their evidence.
See the [correction record](../changes/2026-10-11-compose-dialog-regression-fix.md).

## Automatic documentation minimum

`check_docs.py --staged` reads Git blobs from the index and compares with HEAD.
`--base BASE --head HEAD` reads committed head blobs and compares with the Git
merge-base, so unrelated changes on a diverged base do not count as PR changes.
Each meaningful staged commit needs a new note relative to HEAD; a multi-commit
PR can have several related notes, all inspected together in the PR diff.
Deletes/renames count (renames appear as deletion + addition). Required documents
cannot be deleted. All tracked Markdown local file links, reference definitions,
HTML href/src and Markdown heading fragments are checked without network calls.
External URLs and fenced/inline code examples are not fetched or treated as links.
Use ordinary local paths without unescaped parentheses; keep complex examples in
code fences. This is a small stdlib validator, not a full CommonMark renderer.

| Changed paths | Minimum beyond local-link/required-file checks |
| --- | --- |
| Any non-document change | New dated substantive change note, literal changed paths in Scope, updated HANDOFF |
| `app/src/main/**`, `app/schemas/**` | CURRENT_STATE |
| `apk/`, `data/`, `di/`, `ai/`, `packages/`, `service/` under the Kotlin package; MainActivity/MainViewModel/RenkinApplication; manifests and assets | ARCHITECTURE |
| Gradle/build files, wrapper, catalog, `.gitmodules` | CURRENT_STATE and QA BASELINE |
| Workflows, hooks, scripts, `.gitignore`, `.gitattributes` | WORKFLOW |
| Shared validators, hooks or governance workflow | Numbered ADR added/updated |
| A path containing `ProjectHome` | ROADMAP |
| Explicit changed behavior / architecture / milestone / durable decision in note | CURRENT_STATE / ARCHITECTURE / ROADMAP / numbered ADR respectively |
| Docs-only (Markdown, LICENSE, docs text) | No mandatory change note/state/architecture rewrite; links and PR audit still required |
| At most three Android test/fixture paths under `app/src/test/` or `app/src/androidTest/`, plus optional docs | No mandatory full change note or architecture rewrite; real test evidence and PR audit still required |

Four test paths, guard-test changes, or tests mixed with build/runtime changes do
not receive the small-test exemption. UI strings/layout resources conservatively
count as current behavior. Unknown non-doc changes require a note and handoff.
Do not add irrelevant whitespace to satisfy a required document change.

Each new change note has ten populated level-2 sections and four exact impact
declarations specified in [changes/README](../changes/README.md). Empty/comment-only
sections, unfinished markers, an old edited note, and whitespace-only required-doc
updates fail. Milestones/durable decisions cannot reliably be inferred from every
diff: authors must declare them and reviewers verify the declarations.

## Hook and CI behavior

The shell hooks select Python 3 from PATH, fail if missing, and propagate script
errors. pre-commit additionally runs `git diff --cached --check`. Hooks are local,
optional to install and bypassable; no claim of server enforcement follows from them.

[Governance workflow](../../.github/workflows/governance.yml) runs on PR open,
synchronize, reopen, edits and ready-for-review, using contents:read and no secrets.
Checkout uses the PR head SHA, full history and no persisted credentials. Titles
and bodies are read from the event JSON, never expanded as shell commands.

- `commit-conventions`: every commit in base..head plus the PR title; old base
  history is exempt. Syntax is shared with commit-msg.
- `documentation-audit`: offline guard tests followed by merge-base/head docs
  audit, local links and the completed PR audit. Exactly one unindented
  `Audit result: PASS`, `Audit result: PASS WITH RISKS` or `Audit result: BLOCKED`
  line is required. LF/CRLF and trailing horizontal whitespace are supported.
  Missing, malformed, repeated or conflicting declarations fail; an indented
  or malformed extra `Audit result:` line also counts as a duplicate. A single
  `BLOCKED` declaration fails. This applies to the complete raw PR body, including
  any declaration lines left in examples; remove those extra lines before review.
- Existing [Android CI](../../.github/workflows/ci.yml), job `build`: compile,
  Robolectric/JVM unit tests, assembleDebug, APK artifact and failure reports.
  Its triggers, job and build commands are preserved. SDK setup explicitly requests
  `platform-tools`, replacing v3's default request for the unavailable legacy `tools`
  package; see the [CI follow-up audit](../changes/2026-10-11-android-ci-sdk-setup.md).

CI does not validate natural-language truth, visual quality, actual device behavior,
reviewer identity/authority, provider availability or Play acceptance. Scripts and
workflows modified by a PR are themselves reviewable code; required independent
review prevents accepting a weakened gate just because its own CI passes.

## Main protection — observed status

Verified October 11, 2026, 12:16 UTC at the Codex 01 gate and reread at 15:52 UTC
with the same values, after the owner merged
PR #1 as `005cffa6d51b11e15c20e68349c0c4ce0f23cd8f`:

- [Ruleset 24877351](https://github.com/Zsombor0918/RenkinAI/rules/24877351),
  `main-reviewed-and-audited`, is **active** and includes `refs/heads/main`, with no exclusions.
- Main reports `protected: true`. Its classic-protection summary remains disabled;
  the active ruleset, rather than a classic branch rule, provides the protection.
- Deletion and non-fast-forward updates are restricted. Bypass actors are empty,
  and the connector's current user can never bypass the rule.
- PRs are required. Required approving-review count is **zero** and
  `require_last_push_approval: false`; an approving review is not currently enforced.
  Stale approvals are dismissed if present. The previous closure audit observed
  latest-push approval true and a blocked PR; later owner settings and the actual
  merge supersede that observation. No administration write was made here.
- Required checks are exactly `build`, `commit-conventions`, `documentation-audit`,
  all sourced from GitHub Actions (integration id 15368).
- Conversation resolution is **not required** by the saved rule, and strict
  up-to-date status checks are **disabled**. These differ from the recommended
  setup below; do not describe those requirements as enabled. The P1 thread on
  PR #1 is nevertheless verified resolved.
- The owner accepted and merged #1. That acceptance does not approve later feature
  PRs. Explicit authorized acceptance remains required under AGENTS. Required review,
  conversation resolution and up-to-date checks remain manual owner tasks, using
  steps 7–9 below; do not claim them enabled.

The earlier inactive observations are historical, retained in QA. The connector
did not modify administration settings; the owner created the active ruleset.
The setup steps below remain the recommended configuration, while the observed
values above and the live PR merge state determine the actual enforcement.

## Owner setup, click by click

1. Open [RenkinAI settings](https://github.com/Zsombor0918/RenkinAI/settings).
2. Select **Rules → Rulesets → New ruleset → New branch ruleset**.
3. Name it `main-reviewed-and-audited`; set **Enforcement status: Active**.
4. Leave **Bypass list empty**, including administrators, bots and GitHub Apps.
5. Under target branches choose **Add target → Include by pattern** and enter `main`.
   Confirm there are no exclusions that remove main.
6. Enable **Restrict deletions** and **Block force pushes**. Do not enable Restrict
   updates without a compatible merge path; the PR rule below handles direct pushes.
7. Enable **Require a pull request before merging**, set required approvals to **1**,
   enable dismissal of stale approvals, require approval of the most recent
   reviewable push, and **Require conversation resolution before merging**.
8. Enable **Require status checks to pass**, add the exact observed Actions check
   contexts `build`, `commit-conventions`, `documentation-audit`; select GitHub
   Actions as source where offered. Enable up-to-date branch checks. If contexts
   are not yet listed, run this PR once, then select the emitted contexts; never
   substitute a workflow title or invent a context that has not run.
9. Save/Create the ruleset, reopen it and verify **Active**, target `main`, empty
   bypass list, required review, conversations, checks, deletion and force-push rules.
10. Check this PR's merge box: until required checks and approval complete, merge
    must be blocked. Record the rule id/settings and timestamp in HANDOFF/BASELINE.
    Read branch/ruleset APIs again if available; do not test enforcement by pushing
    unwanted code to main. Keep the PR unmerged until the owner accepts its audit.

A sole contributor cannot approve their own PR. Invite an independent reviewer
with suitable repository access before choosing the strict no-bypass setup above;
do not quietly turn reviews off or add an administrator exception to get past it.
If Rulesets is unavailable for the account, use **Settings → Branches → Add branch
protection rule**, pattern `main`, require PR/1 approval, dismiss stale reviews,
latest-push approval, conversations, the same status contexts/up-to-date branches,
**Do not allow bypassing the above settings**; keep Allow force pushes and Allow
deletions disabled. Save and verify the effective rule with the PR merge box.

Official references: [creating repository rulesets](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/creating-rulesets-for-a-repository),
[available rules](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/available-rules-for-rulesets),
[classic branch protection](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches).
Check current GitHub labels/options when applying the settings.


## Codex 03 verification additions

The existing regression runner retains all M1/M2 selectors unchanged, adds project
style/selection UI, data/migration/backup/session ownership and gateway/legacy AI
parity groups, then the same unfiltered compileDebugKotlin/testDebugUnitTest/
assembleDebug. It verifies selector presence, positive counts, zero failures/errors/
skips and nonzero command exits; no idle/timeout bypass or test exclusion. The actual
KSP-generated v19 schema is retained in the evidence artifact for source checkout;
a missing export fails the runner. Original Android/governance workflows and required
contexts are unchanged. New-schema rollback requires a pre-upgrade full backup or a
forward repair; never recommend installing a v18 APK over a populated v19 database.

At the Codex 03 gate, PR #3 is owner-authorized and merged as aefeb1b6. Ruleset
24877351 was reread: active, bypasses empty, PR and exact build/commit-conventions/
documentation-audit required, force/delete prohibited. Approvals remain 0,
conversation resolution/last-push approval/strict checks false. Do not claim these
recommended owner settings were enabled; no ruleset modification occurred.


## Codex 04 verification additions

The same runner retains every M1/M2/M3 selector and adds reference sampler, structured
schema/composer, analysis gateway, journal, real STYLE UI and accepted-analysis
persistence/session groups. The unfiltered complete command still runs last; it must
contain all required selectors too. Each run clears prior report copies so missing
XML cannot borrow an older result. Actual testcase counts must match declared JUnit
counts; malformed/empty/missing/skipped/failure/error/nonzero evidence fails. Offline
fault-injection tests exercise these negative boundaries and are explicitly synthetic,
not Android proof. Genuine KSP v20 export and retained historical v19 are required in
the artifact. No hooks, shared governance validators or original workflow are weakened.

PR #4 is owner-merged as 6aed9e08 on October 12 at 04:45:50 UTC. Its a73c1e02 source
is an ancestor with an identical tree. At the M4 gate the active ruleset still has
empty bypasses, PR/three required contexts and force/delete protection; approvals=0,
conversation/last-push/strict=false. The additional profile-form-regressions job is
required by this task, not yet a saved ruleset context. No administration write occurred.

Final Codex 04 reread on October 12 also reports
`require_extra_approval_for_unattributed_changes: true`. No author/review status is
fabricated and no ruleset write occurred. PR #5's latest final-head audit/checks
determine readiness; owner acceptance and merge remain separate from green CI.


## Codex 05 additive verification

PR #5 is owner-merged as af7bdb10 on October 12 at 16:10:49 UTC; its final aceb691c
checks are historical evidence for the new M5 branch. The runner retains every
M1–M4 selector and adds five trial-domain/gateway/journal/UI/persistence-session
groups, followed by the same unfiltered compile/unit/APK command. Genuine schemas
19/20/21 are retained and a missing current export fails. A new synthetic fault
case verifies this boundary; fixture schema text is never Android evidence.
Regression path filters now also include AI, project/transfer data, DB and schema
changes, so a final HTTP or persistence correction cannot silently skip regression
evidence. Original CI, governance, hooks and exact required contexts are unchanged.
Final-head XML/counts/APK plus the single complete audit declaration remain required.
No live request, main push/merge, ruleset write or independent review is inferred.


## Style DNA test-first stacked development

Branch feat/style-dna-v1 starts from verified PR6 head454bd5ec, with mainaf7bdb10 unchanged. Both owner specifications were read in full. Legacy one-shot analysis and original-first trials already work. New test-only assertions reproduce missing reason categories, coarse-color duplicate bias, the24-reference boundary, absent per-image reports/family grammar and unevidenced modernization guidance. Hosted assertion execution is required before production changes. The three workflow base filters add feat/style-trials-m5 so a stacked draft receives the same unchanged jobs and complete21-group runner. No selector, timeout, test or approval check is removed. Local Android dependency access is unavailable; hosted CI supplies Android evidence. Zero live or paid provider calls. Deep and physical visual A/B remain deferred; no merge or force push.


## Style DNA opt-in development on stacked PR7

New feat/style-dna-v1 starts at PR6 head454bd5ec; PR6 and main are unchanged. D0 adds safe local missing/unrenderable/dynamic/stateful/render/alpha/resource reasons, independent Oklab color duplicate veto and bounded up-to600 local candidates/24 selected images. Legacy sampler defaults120/12 and M4 aggregate12 remain. New algorithmv3 accepts oldv1/v2 journals without rewriting their fingerprints.

D1 Quick uses up-to12 labeled image parts and one report per image, with conservative independent local synthesis. Standard explicitly opts into up-to24 images in six-image groups and one separately listed text synthesis (maximum5 submissions). Same documented gpt-5.4-mini alias/snapshot, explicit low/high detail, strict Responses text.format/store:false, max12288 output tokens/step, bounded5MiB body/128KiB response. Model availability and quality remain unverified; low is not presumed cheaper. Deep and live A/B are deferred.

D2 versioned reports and multi-family drawing grammar use a tagged style_dna payload in existing nullable revision JSON; Roomv21 and full/shareformat6/payload3 stay. Existing V1 rows/history remain unchanged. Explicit human review and Save create an immutable revision with up-to5 selected private refs. Legacy-only readers reject new grammar before restore writes; safe rollback needs a compatible forward reader. Device-local journals stay out of backups and retain global consumed IDs across delete/restore.

D3 M5 optionally freezes target-specific drawing recipes, explicit family/symbol-type choices and DNA fingerprint in compatible optional plan/context fields. Original component C remains exclusive identity; B only style. Original verification and separate paid consent remain. Null-override AI1/AI2 bodies, non-DNA Stability paths, editor, profile, watch, signing/install and historical PNGs remain.

Test-first b2df166a: hosted904 tests with exactly6 new assertion failures; no old failures. Current new implementation/integration execution is pending hosted CI, not claimed green. Local Python52 guards pass; local Gradle/full runner fail before configuration at distribution download. All old21 groups/selectors remain plus one additive DNA integration group. Zero live/paid requests, no device test, merge or force push. Owner visual acceptance remains open.


## Style DNA integration and compilation follow-up

Implementation2bf8ca7e CI38469333350 failed KSP parsing StyleTrialPlan.kt:117 at an extra closing parenthesis; no Android assertions or APK ran. The correction retains all old fields and optional recipes. Existing full M5 prompt assertions are extended with the new modernization prohibition; all prior exact prompt/image/legacy-byte assertions remain. New MockWebServer, real Room/full-share backup/owned-controller and320dp/dark/restoration/source-switch Compose fixtures join the additive DNA group. Renderer fixtures cover dark-on-dark visible artwork, alpha exclusions, state/animation versus real render exceptions; bounded300/600 local scans have50/150/500/1500 synthetic mappings. Retain all21 old groups, with22 total. Symlink companions, partial reports, consumed steps, native-color veto, and explicit manual Save are guarded. Current corrected execution is pending, not claimed passed. Zero live/paid requests or physical visual test.
