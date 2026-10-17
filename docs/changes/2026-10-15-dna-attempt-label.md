# Label history as an attempt without inferring an HTTP submission

## Summary

Use an accurate history heading even when a claimed attempt has zero submitted
steps. Preserve the current plan, record, consent and no-replay behavior.

## Why

Journal reservation can precede credential reading or a durable submitted marker.
Thus a FAILED record may have zero submitted steps. The new history heading must
not imply that every stored record reached the provider. The existing preserved-
history Compose fixture has precisely such a FAILED/PENDING zero-step record.

## Scope / changed files

Behavior impact: changed
Architecture impact: none
Milestone impact: none
Durable decision: none

- `app/src/main/res/values/ux_style_strings.xml`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleDnaComposeTest.kt`

Only wording and an additional retained-test assertion change. No provider,
controller, journal, schema, signing, migration or request-right change.

## Behavior before / after

The heading says Previous analysis attempt rather than Previous submitted analysis.
The actual submitted-step counter remains authoritative. The test explicitly
asserts zero submitted steps and the accurate attempt label, while retaining all
ready/history/FAILED/UNCERTAIN/busy/disabled/zero-call assertions.

## Tests actually run

The original preserved-history fixture passes in the974-case6fd75af2 Android run;
its new assertion is written before the wording correction. The wording previously
lacked analysis attempt. The final head requires the full unchanged mandatory
matrix; no new pass or phone HTTP count is claimed before actual execution.

## Not tested

Actual phone historical POST/billing and physical acceptance remain unproven.
No secure Work credential or verified budget exists; live testing stays SKIPPED.

## Risks

A claimed attempt is not proof of network enqueue or billing. Do not infer zero
billing for uncertain submitted records or permit replay from any heading.

## Rollback

Revert only the wording if required while retaining record bytes, submitted and
consumed markers, all data, both signing keys and the current-plan correction.

## Documentation updated

Current state and handoff clarify that only durable submitted markers provide
submission evidence; historical UI wording cannot establish phone/provider activity.

## Follow-up

Finish the final source's Android/all23/governance and APK verification, publish
its exact artifact/hash on PR8 and leave it draft/open for physical owner testing.
No live request, main/PR6/PR7 write, force-push or merge occurs.
