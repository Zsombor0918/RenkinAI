# DNA report contract correction

## Summary

Align new image-step request constraints with retained strict validation and diagnose future report rejection without private provider prose.

## Why

All21 supplied screenshots were inspected, including duplicate10/11. Standard shows12/24 accepted,18 images submitted,3/5 markers and REPORT_SCHEMA_INVALID; Quick shows12/12 saved once. No rejected raw body exists here. Empty/whitespace observations and too few reports are allowed by the former outbound schema but rejected by the client. IDs/hashes were unrestricted strings. These are reproduced contract gaps, not proof of the unique Samsung trigger.

## Scope / changed files

- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaV1.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/OpenAiStyleDnaGateway.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaCoordinator.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleDnaPanel.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaGatewayTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaWorkflowTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/data/project/StyleDnaPersistenceTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysisTest.kt`

Behavior impact: changed
Architecture impact: none
Milestone impact: none
Durable decision: none

Existing transport, models, formats, quotas, report acceptance and ownership remain. Requests specify minItems=maxItems, frozen ID/hash enums and nonblank/control-free value pattern plus existing240 length. Pairings/uniqueness are independently checked locally. Fixed allowlisted reasons refine existing journal strings; old history is not rewritten.

## Behavior before / after

Invalid reports remain invalid and cannot Save. Future failures distinguish JSON limits, fields, schema, enum, provenance, text, count, binding format, membership, duplicate ID and hash. First-two-batch reports remain readable after a third-batch rejection, with uncertain consumed request evidence and zero automatic continuation/replay. UI keeps precise codes in Details with readable report-stage explanation. Envelopes and synthesis remain separate.

## Tests actually run

Test-only b7a30380 preserves actual2 tests/2 failures/0 errors/skips at baseline71b7bce in22.920s. Corrected reliability-a-green passes19/19,0 failures/errors/skips in108.762s on Windows/JDK17/SDK36/Gradle8.11.1. Commands/evidence are retained outside Git under task tools2026-10-15. Existing classes and assertions remain; old generic-code expectations are replaced by exact more-specific codes. Eighteen new report fixtures check rejection; valid reordered reports and nonempty UNKNOWN pass. Full REST echoed schemas, loopback transport and real Room Save/reload remain covered.

Official [Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=responses), fetched October15, documents supported enum/pattern/minItems/maxItems. No provider call validates account/model behavior here.

## Not tested

Physical Flip8/provider reproducibility, actual response body, billing and visual fidelity are NOT RUN/unavailable. Final-head full local/CI checks and APK are subsequent gates, not inferred from these focused results.

## Risks

Provider constraints reduce preventable mismatches but cannot prove semantic correctness, exact paired hashes or device success. Client strict validation stays authoritative. No credentials, response body, IDs, paths, images or exception prose enter diagnostics. Legacy AI1/AI2, classic editor, original identity, persistence/backup, atomic/crash/symlink assertions and signing stay intact.

## Rollback

Use a compatible feature-branch revert or forward correction. No Room migration or history rewrite exists. Preserve consumed IDs, saved reports, app data and both signing keys; never replay the failed plan.

## Documentation updated

CURRENT_STATE,HANDOFF,ARCHITECTURE and QA baseline/matrix separate demonstrated contract parity from the unknown phone defect. Existing architecture/milestones/ADRs are unchanged by a narrower request and diagnostic code.

## Follow-up

Proceed incrementally to pack guidance/reference consistency and final-head all23/unfiltered/governance/CI/APK checks. Publish verified source SHA and clickable APK artifact checksum on existing draft PR8; leave owner physical acceptance explicit and do not merge.
