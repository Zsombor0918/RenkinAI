# PR8 provider-result diagnosis: test-first reproduction

## Summary

Continue existing draft PR8 from 1bf21f0e. Production code remains unchanged at
this checkpoint; additive tests distinguish historical UI and processing stages.

## Why

All three owner images were read: 667.jpg (16:57, Quick12), 665.jpg (16:56,
Standard24) and 658(1).jpg (10:14, earlier preparation). Newer images match the
no-current-plan branch, which displays retained FAILED history above previews.
They cannot prove which phone response or local synthesis operation failed.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleDnaComposeTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaGatewayTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaWorkflowTest.kt`

Acceptance requires observed RED before runtime changes, then narrow truthful
history/current-plan wording and stage classification with all validation intact.
Preserve legacy AI1/AI2/Stability, original identity, style-only refs, histories,
Room19/20/21, backup/watch/provenance, consumed rights and both signing paths.

## Behavior before / after

No runtime changes yet. New assertions require preview-only/no-plan status,
history under Details, honest old unknown causes, documented incomplete versus
malformed envelopes, and report validation versus local/Standard synthesis codes.
Schema-valid excessive family diversity remains rejected, never collapsed.

## Tests actually run

Linux/OpenJDK17 local mandatory Gradle attempt fails before configuration because
Gradle8.11.1 distribution download reports Network is unreachable. Zero Android
assertions ran locally. Hosted current-checkpoint RED execution is required next.

## Not tested

Physical Flip8, actual installed resources, Keystore decryption, phone HTTP and
billing history remain unobserved. No live/paid request is authorized or run.

## Risks

INVALID_OR_INTERRUPTED conflates report parsing, local synthesis and persistence
or interruption; it does not prove malformed provider data. Thumbnail count does
not establish a current plan. Consumed markers and historic reason bytes stay.

## Rollback

Revert only these additive tests if needed on the same topic branch. No source,
data, signing identity, journal or request permission changes at this checkpoint.

## Documentation updated

This new record establishes exact authorized scope, preserved paths and actual
local blocker. Canonical runtime documentation is unchanged until a justified fix.

## Follow-up

Observe hosted RED, implement only reproduced corrections, verify final exact-head
Android/all23/governance/XML/schemas and APK, then publish artifact and hash on PR8.
Leave draft/open; main/PR6/PR7, merge and force-push remain outside this task.
