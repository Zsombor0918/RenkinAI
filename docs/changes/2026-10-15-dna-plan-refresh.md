# PR8 current DNA preparation, history and Responses verification

## Summary

Continue only draft PR8 from verified remote76387e2e. Separate a current frozen
DNA plan from older submitted history and fix an actually reproduced state-refresh
race. Preserve existing provider bytes, journals, consumed rights and data. This
is not guided Stage2/3, another refactor, or device/provider acceptance.

## Why

The complete owner specification and six supplied images were inspected before
runtime edits. Two02:59 images show earlier local validation, two09:17 images show
the same local read failure, and10:13/10:14 show distinct populated source samples.
The10:14 image also shows local preparation and the failure text simultaneously.
Those images do not identify a submitted HTTP request, a provider rejection or a
charge. The failure sentence is rendered only from dnaRecord.status==FAILED;
it is not produced by ReferenceSampler or by proof that the current plan failed.

Kotlin evaluates the receiver of copy before its arguments. Both preparation and
refresh previously captured flow.value before a suspending credential read. A
late refresh could therefore restore busy=true and erase a plan which completed
during that read. It could also replace newer submitted progress with older disk
history. This matching path is deterministic offline evidence; the phone's exact
event order, historical reason and submission count remain unproven.

## Scope / changed files

Behavior impact: changed
Architecture impact: none
Milestone impact: none
Durable decision: none

- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysis.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleDnaPanel.kt`
- `app/src/main/res/values/ux_style_strings.xml`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysisTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleDnaComposeTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/project/StyleDnaPersistenceTest.kt`
- `.github/workflows/ci.yml`

No database/schema/migration, build ID, dependency, signing configuration, provider
body/model, sampler selection, trial, editor, backup, approval or source-identity
change. Existing CI tasks remain; an additive SDK35 verification step checks the
actual debug package/signature/hash before its existing artifact upload.

## Behavior before / after

All suspending reads finish before atomic state merges. Refresh owns history and
key presence, never preparation/network/job flags. It retains newer in-flight or
completed record updates instead of installing an older read. Free preparation
retains previous history while giving a new plan its own status and next action.
The review action is above thumbnails. Changed mode/model/detail/draft/source
stamp/revision or unmatched sample hashes disables old consent with recovery text.
Historical failures stay accessible under Details, independently of current ready
or preparing status. With no new plan, a previous failure gets a clearly labeled
summary, safe cause and no-replay guidance. Existing reason/marker bytes survive.

The existing ObserveCoverageInvalidation ON_RESUME observer already invokes the
panel's local refresh even without a source. It is retained, not replaced by a
second lifecycle mechanism. Presence means a saved credential exists; it does not
prove decryption, account permissions or provider acceptance. The gateway still
reads the current encrypted credential only for separately confirmed submission.

| State / event | Current presentation and transition | Deterministic evidence |
| --- | --- | --- |
| Cold entry, no key/history | Free scan/manual Save available; no plan or POST | Owned no-key test; Settings/resume Compose test |
| Quick local prepare | Up to12 frozen independent hashes; ready/no-key reason; one paid step only after confirmation | Refresh-overlap regression; real Responses/Room test |
| Standard local prepare | Up to24 samples; maximum five individually listed steps within separate explicit plan consent | Retained Standard consent test; real Responses/Room test |
| Add/replace/remove key and resume | Existing observer refreshes hasKey and preserves the exact plan; no preparation/submission | settingsKeyChangesRefreshOnResumeWithoutRepreparingOrSubmitting |
| FAILED/UNCERTAIN plus new plan | Current plan is ready; old record stays under Details, with no replay rights | freshPlanIsReadyWhileFailedOrUncertainHistoryStaysUnderDetails; HTTP fault controller test |
| Source/model/detail/style changes | Old confirmation disabled; free rescan/reopen required; coordinator independently rechecks context | Changed-options/source-confirmation tests; retained stale coordinator/owner tests |
| Synthetic completed response | Actual gateway/coordinator/journal produce bound reports and RESULT_SAVED; explicit reviewed Save creates immutable Room revision, reloads durably | quickAndStandardUseRealResponsesWireThenReviewSaveAndReloadWithoutReplay |
| Auth/schema/timeout/cancel/recreation | Safe reason, durable submitted/consumed IDs, no implicit Save, no resend; uncertain billing stays uncertain | HTTP fault/cancellation/refresh-progress tests; retained cold/marker/symlink tests |

Existing Standard consent authorizes only its explicitly enumerated frozen steps;
there is no new authorization per automatic app open/resume or failed attempt.

Official provider documentation was read on October15: [model](https://developers.openai.com/api/docs/models/gpt-5.4-mini),
[vision](https://developers.openai.com/api/docs/guides/images-vision) and
[structured outputs](https://developers.openai.com/api/docs/guides/structured-outputs).
The alias/snapshot, image input, Responses, reasoning none, explicit low/high and
strict text.format contract are documented. Existing application requests retain
store:false, separate labeled base64 PNGs,12288 output-token cap,5MiB request and
128KiB response bounds and one-shot/no-redirect/no-retry transport. Quick local
synthesis and Standard reports/text-only synthesis remain unchanged. Documentation
does not certify account availability, real response quality or a phone request.

## Tests actually run

Test-only948f805ddbde3f31c40adaeadae1de07f03b6f83 precedes runtime edits.
[Android38706159146](https://github.com/Zsombor0918/RenkinAI/actions/runs/38706159146)
compiled production and tests, then ran969 tests with precisely three new failures:
refreshOverlappingLocalFreezeCannotLoseTheNewPlanOrRestoreBusyState and the new
ready/history and changed-option Compose assertions. No earlier case failed.
The original assertions remain unchanged. Failure-report artifact11958498226 has
224150bytes, SHA256b3cb901b02a6b9e05eb699a90da7f63af1a7ee5309c4b5e61eb071f0c70b0370.

Corrective full-suite/all23/CI and local governance results must be verified on
the exact committed head and recorded in PR8 after committing. They are not yet
claimed by this implementation checkpoint. Added tests exercise real Room and
loopback MockWebServer through the production controller/coordinator/journal:
Quick12/one POST and Standard24/five POSTs, independent image/label/hash bytes,
durable submitted markers before request headers, credential refresh, candidate
review/Save/reload and no replay. Concurrent tests use explicit barriers, not sleeps.
Auth/schema/timeout and actual coroutine cancellation preserve consumed rights.
No original selector, assertion, timeout, idling rule or security guard is removed.

Local Linux/OpenJDK17 wrapper attempt fails downloading Gradle8.11.1 with Network
is unreachable before configuration. Hosted JDK17/SDK/native Robolectric/Compose/
Room/MockWebServer provide the Android execution evidence. This limitation is not
misreported as a local Android pass.

## Not tested

Physical Flip8/One UI, actual installed-pack resources, Android Keystore decryption,
real phone process kill, TalkBack/fold and installed-signature upgrade remain NOT RUN.
No approved ephemeral OpenAI credential or verified account spend controls are
available in Work. The optional live test is SKIPPED: zero live/paid requests, no
icon generations, no key requested or copied from the phone. Historical phone
billing/submission cannot be inferred from screenshots or these offline tests.

## Risks

The actual phone event order and old journal reason remain unproven, even though
the matching race and misleading presentation are reproduced. A ready plan is not
an API result. A stored key is not account access. Old failures and uncertain steps
remain non-replayable; no cleanup, reset or speculative repair is performed. CI's
debug signer may differ from the installed app; no owner key is replaced and no
uninstall/data-clear instruction is an acceptance step.

## Rollback

Use a compatible reviewed topic-branch revert or forward repair retaining Room21,
tagged DNA readers, immutable history, originals/RAW, consumed markers and both
signing keys. No destructive downgrade, data reset or paid replay is required.

## Documentation updated

Canonical state, handoff, architecture, workflow and QA describe this bounded race,
current/history presentation, unchanged lifecycle refresh and additive APK check.
The owner checklist keeps free preparation/Settings/manual Save separate from a
later optional paid confirmation. INDEX links this record; historical notes remain.
No milestone or durable architecture decision is changed.

## Follow-up

Finish mandatory checks on the final commit, independently inspect retained XML/
schemas and the newly downloaded APK against SDK signing/hash output, then publish
the exact final SHA/artifact/SHA256 on PR8. Keep its audit BLOCKED until automated
gates complete; owner physical acceptance remains NOT RUN even if PASS WITH RISKS
becomes justified. Leave PR8 draft/open; no merge, force-push, main/PR6/PR7 write,
ready-for-review transition, Stage2 experiment or Stage3 cleanup is performed.
