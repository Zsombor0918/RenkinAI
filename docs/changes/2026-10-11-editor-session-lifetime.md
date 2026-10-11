# ProjectHome — retain editor result registrations while loading

## Summary

Keep the existing editor composed on its visible route during profile loading,
behind a loading/input/accessibility barrier. Bound the reused profile form fields
and clear stale list data on retry. Add an external-result lifetime regression.

## Why

Disposing MainColumn when a watch/profile switch changes readiness unregisters
its GlobalOptions/WallpaperPreview result launchers. Recreating it with a different
profile key can lose an in-flight result. Two new form tests also exposed Compose
idle failures in the real reused dialog; the form now has an explicit bounded width.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: recorded

- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/RenkinAiRoot.kt`:
  stable editor composition across load/ID changes, concealed loading content with
  blocked pointer/semantics access, and reset the profile list on a retry.
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/ProfileSwitcher.kt`:
  fill the available bounded dialog width for the existing three-field form.
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectShellComposeTest.kt`:
  register and dispatch an actual ActivityResultRegistry callback during loading
  and an early active-profile ID change, then assert the editor resumes.

The stable editor-lifetime decision is clarified in ADR 0002, still proposed for
owner acceptance. M1 scope/status does not advance another milestone. No change
to the profile store, session owner, Activity-result contracts or pack/AI pipeline.

## Behavior before / after

An Editor route used to remove/rekey MainColumn while the provider was loading.
It now retains launchers and saved UI primitives under one editor key; loading
content is concealed and cannot receive ordinary taps or accessibility actions.
Home still disposes the editor; Home-triggered profile operations still wait for
successful baseline completion before navigating. Failed list retry shows loading
rather than the old list. The profile form retains the same validation/actions.

## Tests actually run

At preceding head `4c218c0e8e3862dfe9ddcd033bb27a7853ba6587`, Android CI
38141510224 passed app/test compilation and ran 627 tests; **2 FAILED** with
AppNotIdleException immediately after opening the profile form. The downloaded
failure reports confirm these exact shell tests; the debug APK was not built.
Local governance previously passed 45 tests; follow-up staged/range checks and
complete Android results will be recorded after execution in QA and the PR audit.

## Not tested

This follow-up has not yet passed a fresh Android run. Local Gradle download is
blocked, and no physical device/emulator is present. Full MainActivity, actual
GlobalOptions/WallpaperPreview return during a watch switch, TalkBack and OEM
window behavior remain NOT RUN. No live paid provider call was made.

## Risks

Retained editor effects and dialog windows behave as in the original loading
session; the shell barrier controls root content, not separate OS Activity windows.
Fake-profile tests with a real result registry prove registration lifetime, not
all device lifecycle timings. Form idle failures must actually pass fresh CI.

## Rollback

Revert through a reviewed PR, or roll back M1 to MainColumn as Activity root.
No data/key/schema migration is involved. Keep existing profiles and user data.

## Documentation updated

CURRENT_STATE, ARCHITECTURE and ADR 0002 describe retained editor composition.
HANDOFF, BASELINE and TEST_MATRIX record the actual unsuccessful preceding suite
and the new external-result regression; earlier counts remain historical.

## Follow-up

Run the complete Android and governance workflows on this follow-up, resolve any
remaining form-idle issue, and record final-head evidence before making the draft
ready for owner review. Complete the device matrix separately before acceptance.
