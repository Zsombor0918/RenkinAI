# Codex 03 verified checkpoint and diagnostic removal

## Summary

Record complete 718-test/API-fixture/Android/APK success and remove temporary
selection diagnostics. Consolidate current canonical state and next-agent handoff;
failed history remains immutable in QA and the earlier change records.

## Why

Head 88933c95 passed the actual full and isolated suites and APK. Archived output
proves the previous fake version comparison false at initial zero, and correctly
typed Long produces one Save and exact keys. Probes now served their purpose;
retaining verbose semantic dumps would obscure future failures without more value.

## Scope / changed files

- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectSelectionComposeTest.kt`

Behavior impact: none
Architecture impact: none
Milestone impact: changed
Durable decision: none

Only temporary test print output is removed. Real clicks, enabled/displayed checks,
Save completion/exact-key/count and restoration assertions remain unchanged.

## Behavior before / after

Tests retain the corrected Long optimistic-version contract and all user interaction
coverage without printing fake metadata/semantic trees. Canonical docs distinguish
implemented source, the verified 06e71532 legacy-compatible checkpoint, exact final-
head checks in the PR audit and still missing owner/physical-device acceptance.

## Tests actually run

Android 38176787953 and regression 38176787903 on 88933c95 succeeded. Downloaded
artifact 11716765047: full 717/717, no failure/error/skip; original cases 1/1, 1/1,
2/2, 3/3, Create/Extend 10/10, coverage 28/28, new UI 12/12, data 30/30, gateway/legacy
31/31 all exit 0. assembleDebug passed in both workflows.

Head 06e715323f4358cbb08abf8766e56b64547924ea: Android 38177165898 and regressions
38177165977 PASS, debug APK artifact 11716760691. Downloaded artifact 11716575779:
127 full-suite XML independently counted 718 tests, zero failure/error/skip; data
31/31 including real format-3 large/transparent gallery PNG restore. All other
isolated groups retain the counts above and pass with no missing selectors. KSP
v19 export hash matches committed schema. Local guards 45/45 in 16.053 s. This
last diagnostic-removal head requires fresh checks recorded in the PR audit.

## Not tested

The final diagnostic-removal HEAD has not run yet when this note is committed;
its exact SHA/checks are recorded in PR #4 after publication. Devices, TalkBack/OEM/Flip, URI providers, physical upgrade/SAF/
keystore/generated-pack update/install remain NOT RUN. No live paid API requests.

## Risks

No runtime change in this commit. Physical binder/window/storage/OEM behavior is
not established by native Robolectric or MockWebServer. Keep those device gaps
explicit and require owner/reviewer acceptance after final technical checks.

## Rollback

Restore diagnostic prints only if new evidence warrants investigation. Do not
change typed expected version or drop assertions. Runtime v19 rollback remains
forward repair or explicit pre-upgrade-backup recovery with stable signing.

## Documentation updated

CURRENT_STATE and ARCHITECTURE consolidate source-backed contracts. HANDOFF names
actual checkpoints and next gates. BASELINE preserves the failed and successful
heads; TEST_MATRIX records automated boundaries separately from device NOT RUN.
ROADMAP marks the verified technical checkpoint, still pending owner/device acceptance.

## Follow-up

Verify this exact final head and the genuine legacy-gallery fixture through every
original/new selector and complete compileDebugKotlin/testDebugUnitTest/assembleDebug.
Then update the single PR audit from actual results and leave acceptance to owner.
