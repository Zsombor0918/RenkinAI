# PR8 final verification completion

## Summary

Close only two demonstrated verification boundaries after the controller/UI fix:
an unavailable hosted-runner command and a retained post-restoration scroll race.
Application runtime remains identical to implementation `4be6ad63`.

## Why

At `6fd75af2`, Android CI executes all **974 tests successfully**, including the
three original red cases, key add/replace/remove on real resume, real Responses/
Room Quick12/Standard24, explicit Save/reload and no-replay/fault/cancel tests.
The existing lifecycle observer works once the fixture commits its Compose effect
through ordinary idling; replacing the lifecycle registry alone did not resolve
the earlier observation. Event and invalidation counts remain explicit assertions.

The APK assembles successfully, but the new verification step fails because the
hosted runner has no `rg` executable. Separately, independent regression evidence
has all22 isolated groups passing, but its last full974 run fails one retained
guided assertion: after restoration, the awaited initial Room load/reflow moves
the already materialized free-scan button offscreen. The exact displayed assertion
is retained; a green standalone unit run cannot replace this failed full command.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

- `.github/workflows/ci.yml`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/GuidedProjectRoomJourneyComposeTest.kt`

The runner command uses available standard grep with an anchored exact package
pattern. The fixture realigns the same lazy-list control after its existing
enabled wait and ordinary idling, before unchanged displayed/enabled/click checks.
No runtime, timeout, idling policy, selector, assertion, schema or signing change.

## Behavior before / after

The additive APK gate keeps nonempty/package/apksigner/hash verification and all
original CI tasks. It no longer assumes ripgrep is preinstalled. The guided helper
first materializes the control, awaits real readiness, commits layout and scrolls
that same control into view. Restoration, current plan, exact stamps/version/
selection, no-key gate and zero-provider-call assertions all remain unchanged.

## Tests actually run

[Android38711653859](https://github.com/Zsombor0918/RenkinAI/actions/runs/38711653859)
at `6fd75af26fe04595719bf618b19b173b96084f53`: compile/test **974/974**, assemble
PASS; APK verification fails at `rg: command not found` before signing/hash/upload.
Downloaded test report11961232344 is221888bytes, SHA256
`a45ef56f42e8ff7363b635f89f0a1a48fb5a45aff952ca527fd61d49eb31f205`.

[Regression38711653794](https://github.com/Zsombor0918/RenkinAI/actions/runs/38711653794)
artifact11961372299 is683404bytes, SHA256
`46a04221c7e134f5f6812dec67f761501114c7d46bedcce3b76e366abc35a234`.
Independent local recount verifies exact HEAD, all23 commands/selectors,154 full
XML and974 actual cases: one full-suite displayed failure, zero errors/skips.
Every isolated group passes, including DNA34, analysis persistence/session31 and guided35.
Actual counts are in the artifact; the final source must pass both complete runs.
Retained19/20 and KSP21 schema bytes match tracked files exactly.

Local Python55 governance tests pass at the implementation checkpoint; staged/range
docs, subjects and real hooks pass. The final committed head still needs its own
mandatory hosted/local checks and fresh APK, recorded externally on PR8 after
execution rather than inventing a file containing its own hash.

## Not tested

Physical Flip8/One UI/Keystore/process kill/TalkBack/fold and installed-signer
compatibility remain NOT RUN. Optional live API testing is SKIPPED: no secure Work
credential or verified account spend controls exist. Zero live/paid provider calls.
The phone's old submission/billing count and exact event order remain unproven.

## Risks

A passing standalone974 run is not evidence that the later full-suite order passed.
The failing reflow assertion and verifier step must both pass on final HEAD. No
gate is disabled to obtain an APK. CI signing remains unchanged and can differ
from the installed app; preserve data/keys and never require an uninstall/reset.

## Rollback

Use a compatible topic-branch revert or forward repair retaining Room21/tagged
readers, immutable styles/targets/trials/originals/RAW, consumed request evidence
and both signing keys. No destructive downgrade or paid replay is required.

## Documentation updated

Canonical state/handoff/QA now distinguish the974 Android success from the retained
full-order scroll failure and unavailable CI command. Workflow documents the same
APK verifier with its available utility. The original diagnosis/state-transition
table and every failed checkpoint remain reviewable; no milestone/ADR changes.

## Follow-up

Run all mandatory checks on this final source, independently recount its XML and
schema evidence, download and match the exact APK to SDK signature/hash output,
and publish that commit/artifact/SHA256 on PR8. Keep the audit BLOCKED until those
gates pass, then use PASS WITH RISKS only with physical acceptance NOT RUN. Leave
PR8 draft/open for the owner's checklist; no merge, force-push, main/PR6/PR7 write,
new stage, icon generation or live API request is performed.
