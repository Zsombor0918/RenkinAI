# Derived guidance and family references reproduction

## Summary

Retain two failing offline assertions before the bounded guidance correction.

## Why

The screenshots show seven tiny families and unhelpful unknown prose, with reference selection unrelated to the reviewed family. Existing raw reports must remain intact.

## Scope / changed files

- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaWorkflowTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleDnaComposeTest.kt`

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

## Behavior before / after

Production remains04dfa532. A derived overall selection is currently rejected; choosing the second family still saves the first five unrelated images.

## Tests actually run

Windows existing JDK17/SDK36/Gradle8.11.1: fidelity-bc-red compiles and runs2 tests/2 assertion failures/0 errors/skips in22.025s. UI reaches Save with five wrong-family references. Evidence stays outside Git under task tools2026-10-15/fidelity-bc-red.

## Not tested

No provider request or generated-image fidelity comparison is authorized or executed.

## Risks

Derived normalization must stay conservative, evidence-bound and unable to borrow source identity. Existing stored grammar and histories must round-trip unchanged.

## Rollback

Use a feature-branch revert without changing saved data, request markers or signing keys.

## Documentation updated

This scoped red note records the test-only checkpoint under the small Android-test exemption.

## Follow-up

Implement a compatible derived view, matching deterministic reference selection and narrow presentation, then verify final source and all retained contracts.
