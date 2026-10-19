# Preserve available Trial preview detail on dense screens

## Summary

Correct the demonstrated review-only density error while retaining original and
generated PNG bytes, financial consent and the existing EXTEND workflow.

## Why

Owner screenshots show pixelated enlarged previews. The native decoder regression
demonstrates128 decoded pixels for128dp at3x density, where384 physical pixels are
needed. This independently explains avoidable preview degradation, not provider
output quality or native source resolution.

## Scope / changed files

Behavior impact: changed
Architecture impact: none
Milestone impact: none
Durable decision: none

`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectTrialPanel.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleTrialComposeTest.kt`

Extract the existing decoder, account for display density, perform decode work off
the main thread, reset published state on input changes and preserve Compose-owned
bitmap lifetime. Existing UI controls and the shared test harness remain unchanged.

## Behavior before / after

Before, the decoded128px result is enlarged to384px on a3x screen;48dp previews have
the same problem. After, bounded sampling retains at least the physical destination
detail available in the saved source. PNG inputs/results, provider requests and hashes
are untouched. No sharpening, invented detail, forced higher model or extra request.

## Tests actually run

Windows/JDK17/native Robolectric initially times out in window capture before any
pixel assertion; retained as an unsupported fixture result. Extracting the actual
decoder without behavior change then gives a focused RED: expected at least384,
actual128. Corrected native pixel regression passes1/1, preserving fine red/blue
stripes and the source hash; before/after PNGs were visually inspected. The same
three-case invocation deliberately executes two separate contract/journal REDs for
the next increment. One earlier local compile was interrupted before assertions;
no successful result is claimed for it. Final exact-head checks belong in PR10.

## Not tested

No owner RAW file, source APK, installed data, API key or physical phone is accessed.
Screenshot JPEGs cannot establish actual provider output resolution or alpha. No paid
generation or real aesthetic comparison occurs.

## Risks

Dense previews decode larger bounded bitmaps; they still downsample rather than
upscale missing detail. Compose may retain published pixels briefly; never recycle
them while draw nodes can reference them. Signing and historical governance risks
remain independent and must not be relabeled successful by an image test.

## Rollback

Use a reviewed ordinary revert of the preview code if needed. No stored assets,
plans, approval, billing journal, migration or signing change requires rollback.

## Documentation updated

Current state, QA baseline/matrix and handoff distinguish the measured presentation
defect from unknown native source quality and unverified generated aesthetics.

## Follow-up

Finish the separately authorized versioned prompt and pricing comparison on the same
branch/PR10, then one final-head Linux/Android/Python verification and verified APK.
Keep old requests intact and leave actual visual comparison to the owner.
