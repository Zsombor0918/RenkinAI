# Single-icon trial contract — test-first checkpoint

## Summary

Add executable regressions before changing production source on the existing
feat/style-trials-m5 / PR #6. Baseline is a1c2310895bd3f161da31ff03b72575b2853e917;
accepted main remains af7bdb104869b0d0dded2acdf020fc030642dcf9.

## Why

Owner ticket [6142499049](https://github.com/Zsombor0918/RenkinAI/pull/6#issuecomment-6142499049)
reports multi-logo trial collages on Flip8. M5 overrides the stronger legacy prompt
with generic saved-style text. The screenshots are not in the PR; no visual inspection
or live reproduction is claimed here.

## Scope / changed files

- `app/src/test/kotlin/dev/renkinProject/renkin/ai/trial/StyleTrialGatewayTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/AiImagePipelineTest.kt`

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

Only two existing test classes and documentation change. No runtime, runner,
schema, timeout, idling or original assertion changes.

## Behavior before / after

Production is deliberately identical to a1c2310. New tests observe real coordinator,
journal, Room and serialized native multipart inputs for targets A/B/C, 0/1/5 refs,
saved manual/MIXED styles and conflicting instructions. They require one authoritative
target/edit/output contract, quoted appearance-only guidance and bounded output.
Independent pre-fix snapshots compare complete legacy AI1/AI2 and Stability body bytes
using the actual random MIME boundary. All legacy option strings are pinned.

## Tests actually run

Linux/Python 3.12/JDK17: python3 -m unittest discover -s scripts/tests -v passed
52 tests in 7.319 seconds. git diff --check passed. The local command
./gradlew compileDebugKotlin testDebugUnitTest assembleDebug --console=plain failed
before configuration downloading Gradle8.11.1: Network is unreachable. Hosted
Android and all 21 existing regression commands will execute this test-only head;
their actual failures must be read before modifying production source.

## Not tested

No local Android tests ran; hosted red evidence is pending at this checkpoint.
No provider, owner artwork, device, Samsung/Theme Park or paid image request was used.
These assertions reproduce the objectively missing prompt contract, not model output.

## Risks

Expected red tests prevent automated readiness until the corrective commit. They
must fail for contract omissions, not compilation or unrelated fixtures. Frozen
parity tests must pass before and after the correction. Existing paid PNGs and reviews
must remain untouched; structural PNG checks do not detect collages or logo identity.

## Rollback

Revert this test-only commit if necessary; no persisted data or app identity changes.
Retain equivalent regressions when implementing the correction. Never replay old
paid request IDs or erase owner data to hide a bad trial.

## Documentation updated

BASELINE and HANDOFF identify this deliberately test-first state and the owner
single-icon acceptance blocker. Canonical application architecture is unchanged.
Exact committed SHA and hosted results belong in the PR audit after observation.

## Follow-up

Inspect actual failing JUnit cases on this head, then implement the minimal OpenAI
contract without changing legacy or Stability bytes. Verify the complete final-head
suite, all 21 regression groups, governance and matching debug APK. Leave #6 draft/open
for separately authorized owner image generation and explicit visual acceptance.
