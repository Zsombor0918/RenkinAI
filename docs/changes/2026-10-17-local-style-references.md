# Local pack references without analysis

## Summary

Phase3A.1 adds the owner-protected local sampling/import/Save path on a separate
branch from3831120691217503c868d640a690fef2a7beca88. It is not exposed as a new
default until the guided integration follows.

## Why

Reviewed source-pack PNGs should be reusable without buying a cloud analysis.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: none

`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/LocalStyleReferences.kt`
reuses the sampler, owner and private assets. Draft provenance in
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStylePanel.kt`
and the existing adapter in `app/src/main/kotlin/dev/renkinProject/renkin/MainViewModel.kt`
carry source checks through Save.
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectPreparationTest.kt`
checks bytes/order/reopen/owner and stale/missing-resource rejection.

## Behavior before / after

Manual PNG import remains. The new local service returns up to12 diverse candidates
with no automatic selection. Explicit selection imports only the chosen PNG into
the existing profile-owned asset store. Save rereads source identity and selected
drawable bytes; transient provenance survives the form, not a new database schema.

## Tests actually run

Native Windows/JDK17: `:app:testDebugUnitTest --tests "*ProjectPreparationTest"`
actually executed7/7 cases, zero failures/errors/skips (3m44s including compile).
Final-head authoritative Linux, Python and Android evidence belongs in the new PR.

## Not tested

No live provider, physical phone, visual-quality or installed-signature acceptance.

## Risks

Local sampling is a bounded diversity heuristic, not semantic family recognition.
The user must review candidates; mixed colors do not imply a fixed palette.

## Rollback

Revert this ordinary commit. Imported assets remain compatible with existing
reference readers; no migration, signing or journal change.

## Documentation updated

Current state, architecture and handoff describe the unexposed data increment.

## Follow-up

Complete guided free Save-to-Trial integration before publishing a new default.
