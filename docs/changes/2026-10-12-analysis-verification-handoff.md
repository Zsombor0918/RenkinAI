# Codex 04 — verified implementation and Codex 05 handoff

## Summary

Close canonical documentation around the measured 792-test implementation and
provide the precise next-stage input/recovery contract. No runtime/test/schema change.

## Why

Inherited pending paragraphs and ADR 0003's old proposed status obscure actual
progress. Future agents need the completed scope, failed history, current risks and
mandatory exact-final-head gate instead of replaying the earlier stalled diagnosis.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: changed
Durable decision: none

Documentation only: CURRENT_STATE, ARCHITECTURE, ROADMAP, HANDOFF, INDEX, WORKFLOW,
BASELINE, TEST_MATRIX, ADR 0003's observed acceptance status, ADR 0005's accessible
official Responses reference and this note. All non-document paths remain identical
to verified code head 47503e5c, including genuine schemas and every retained test.

## Behavior before / after

M4 is labelled implemented and offline-verified, still awaiting owner merge and
physical/provider acceptance. The handoff consolidates source/consent/route/one-shot
HTTP/immutable-save/backup boundaries. Historical failing 420d0b87 and 53addf96,
791 checkpoint and all M1/M2/M3 records remain in QA and original notes.

## Tests actually run

47503e5c passed Android 38240314705 and regressions 38240314877. Downloaded artifact
11742147999 independently confirms 135 XML / 792 tests / zero failure/error/skip,
all 16 groups, zero exits and every selector. One-shot control passed isolated and
full, proving two repeatable-body mock POSTs versus one analysis POST/UNCERTAIN.
Real APK artifact 11742383649 exists; genuine schema 19/20 bytes match source.
Local 51 Python tests passed in 13.245 s; shared staged/range/hooks passed. This
documentation head's fresh results and SHA are recorded in PR #5 after publication.

## Not tested

No local Android build succeeds: Gradle download fails before configuration.
Physical device/URI/SAF/process/storage/OEM/TalkBack/signing/install and real provider
account/model/cost/quality remain NOT RUN. Live/billable requests remain zero.

## Risks

Heuristic style quality, device-local unreviewed candidates, cross-store legacy
restore and v20 downgrade/signing limitations remain explicit. Green automation
does not establish device/Play acceptance or authorize an owner merge.

## Rollback

Revert documentation only if contrary evidence appears; do not alter historical
test records or remove guards. Installed v20 still requires forward repair or
explicit compatible pre-upgrade full-backup recovery with stable signing.

## Documentation updated

State/roadmap/architecture/index agree about completed code and pending acceptance.
HANDOFF gives concrete Codex 05 inputs; QA records exact artifacts/counts. WORKFLOW
records the fresh active ruleset including its unattributed-change approval flag;
ADR 0003 now matches actual owner #3 merge and ADR 0005 remains proposed.

## Follow-up

Verify this exact closing head through full Android/APK, every isolated group and
all four contexts. Update one complete PR audit to evidence-based PASS WITH RISKS,
verify ready state and leave merge separate. Codex 05 requires freshly validated
style/refs/targets/source plus NEW explicit paid consent; no trial images are here.
