# Freeze a reference-first Trial generation contract

## Summary

Implement owner-authorized Phase4A from integration91e5737d on a separate stacked
feature branch. New OpenAI Trials use V2; historical V1 plans remain unchanged.
No analysis algorithm, model, signing, editor or persistence-store redesign.

## Why

The DNA branch used dnaTrialAppearance(recipe,specification,instruction) even when
specification came from the same compiler. Identical construction rules appeared
twice. V1 also overstated fixed logo proportions and required family evidence
instead of permitting a directly reviewed reference-first style.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: changed
Durable decision: recorded

`app/src/androidTest/kotlin/dev/renkinProject/renkin/offline/StyleDnaEmulatorTest.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/IconGenerationGateway.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/OpenAiImageClient.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/style/PromptComposer.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialCoordinator.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialJournal.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialPlan.kt`
`app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrials.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/PromptComposerTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialGatewayTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialJournalTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialPlanTest.kt`
`app/src/test/kotlin/dev/renkinProject/renkin/ui/project/GuidedProjectRoomJourneyComposeTest.kt`

These paths add immutable version dispatch, effective prompt hashes, conservative
appearance composition and compact existing-suite integration assertions. No test
group, workflow schedule, Room schema, backup or API model is changed.

## Behavior before / after

Before: V1 authoritative wrapper plus target recipe plus overlapping saved recipe,
including family/provenance/unknown prose. New plans: one V2 wrapper plus one quoted
appearance brief; known generated metadata and exact duplicate lines are omitted.
Unique reviewed edits, project instructions and target construction remain. Manual
prose is not summarized. Identity comes only from IMAGE1; ordered IMAGE2?6 supply
style. Redrawing is allowed without losing identity. Dark silhouettes do not imply
background tiles; transparent canvas permits evidenced tile artwork. Reviewed
fixed/mono/duotone rules may recolor; uncertain reference brand hues may not.
Exactly one1024x1024 transparent PNG and existing native multipart fields remain.

Missing promptVersion remains V1, with no added canonical fields. New V2 optional
version/hash fields bind every full effective UTF-8 prompt to the plan fingerprint.
Readers reject unsupported versions or text/hash mismatch before request claim.
Coordinator and native factory dispatch the same version; old journal UUID/endpoint
and global consumed ledgers still prohibit replay, including version retagging.
Deleted V2 records rehash only redacted contents while retaining consumed evidence.

## Tests actually run

Native Windows/JDK17: focused guided Save-to-Trial test expected RED (1/1 failed,
new-plan version expected2/actual1),47 seconds. The first duplicate fixture attempt
failed before composition because nonnative PNG fixtures collapsed; this is not
claimed as the intended duplication RED. The corrected compact fixture explicitly
reproduces two V1 rule occurrences and requires one V2 occurrence. Initial focused
execution72 cases had70 PASS/2 fixture failures: multipart header parsing and an
independent golden calculation omitting Android JSON slash escaping. Both fixtures
were corrected without weakening original compatibility or request assertions.
Python67/67 PASS in52.792s. A later72-case run passed71; the new maximum-escaping
V2 case exposed redundant V1 preflight validation rejecting an otherwise bounded
V2 request. V2 now validates its own effective contract; V1 retains the exact old
validation. The failing artifact retains the V1 composer stack. Final focused
result: six existing classes72/72 PASS, zero errors/skips, on the stable source in2m49s including recompilation; Android test Kotlin compilation also PASS. V1 compatibility/control and actual V2 wire/consent/restart cases execute together.

## Not tested

No live/paid generation or analysis and no actual API-key access. Physical Samsung,
OEM pristine-icon guarantees, installed upgrade signing and aesthetic fidelity are
not established. Final authoritative hosted Linux/build and Android results belong
in the exact-head PR audit after commit, not inherited baseline counts.

## Risks

Conservative line equality does not semantically summarize paraphrased manual
prose. Historical compiler metadata is recognized narrowly; unique edited lines
are retained. V2 must never be edited in place: future wording needs a new version,
otherwise stored hashes fail closed. Old app versions cannot read V2 fields; retain
a forward-compatible reader for rollback. Existing CI debug keys are ephemeral;
installed update compatibility needs public-certificate verification separately.

## Rollback

Use ordinary reviewed feature-branch revert/forward repair. Keep V2 readers and
consumed markers for already stored records; never downgrade by deleting journals,
rewriting fingerprints, replaying uncertainty or clearing user data. No migration
or private-key replacement is introduced.

## Documentation updated

ADR0014 records the immutable contract and conservative deduplication decision.
INDEX, ARCHITECTURE, CURRENT_STATE, HANDOFF, ROADMAP, WORKFLOW, TEST_MATRIX and
OFFLINE_STYLE_DNA record the bounded phase and exact-head verification boundary.
Historical Phase3A pending prose remains evidence of earlier states.

## Follow-up

Complete one stable final-head hosted full suite/build, Python/governance and the
existing five Android journeys. Independently verify/publish the fresh APK and
signer limitation in the new draft PR, keep it open/unmerged and stop. Phase4B
analysis-entry retirement and any paid visual pilot require separate authorization.
