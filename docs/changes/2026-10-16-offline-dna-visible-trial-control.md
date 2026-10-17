# Observe the real trial control before the instrumented click

## Summary

Finish the existing offline emulator fixture with one bounded UI observation
helper. No production code or application behavior changes in this follow-up.

## Why

Exact6479b8f7 hosted Standard fails while scrolling for trial-prepare in the
project-trials-1 loading list. Quick and incomplete/cancel pass; restart fails
only because Standard never writes its completion evidence. The retained Standard
journey is the failing regression, not a claimed passing historical checkpoint.

ProjectTrialPanel always creates the list but adds the prepare item only when its
collected preparation is nonnull. OwnedStyleTrials.load temporarily clears that
preparation. The fixture waited for controller readiness and immediately scrolled
the UI; it did not await Compose's materialized control. TargetSelection awaits
the Room selection write before navigation. The unique frame/reload ordering is
unproven, but the failed boundary is a test synchronization assumption, with no
evidence requiring a production controller or persistence change.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

Only `app/src/androidTest/kotlin/dev/renkinProject/renkin/offline/StyleDnaEmulatorTest.kt`
changes. Canonical QA/handoff record the observed failure and final delivery gate.

## Behavior before / after

Both trial preparation sites now observe controller readiness and materialize the
actual enabled trial-prepare node within the existing15-second bound. An absent
item during loading remains an observation to await, not a successful assertion.
The helper then performs exactly one displayed/enabled click and awaits the actual
plan/error, preserving every subsequent identity/reference/marker/POST assertion.
Persistent absence, error or lost plan still fails. No sleep, timeout increase,
skipped test, forced state, provider retry or automatic UI-action retry is added.

## Tests actually run

Downloaded actual failure artifact12028567503 from hosted run38859576864 retains
the6479 Standard missing-node stack and dependent restart failure. Existing local
evidence passes the earlier fixture, so that result does not erase the hosted RED.
The corrected focused standardJourney passes1/1 in11.441s on the existing API35
emulator, then1/1 in11.155s at320x640/density160 matching the failed hosted width.
Only the dedicated emulator's display override was changed and reset afterward.
Cached assembleDebug/assembleDebugAndroidTest/fixture assembly succeeds. These
focused passes do not substitute for final committed-head full/hosted checks.

## Not tested

No live/paid API, physical phone, Samsung fidelity or installed-data signing upgrade
is tested. The exact scheduling order behind the hosted transient is not inferred
from local success. Final hosted execution must pass before closing the audit.

## Risks

The bounded observation may time out for a genuine persistent unavailable control;
that remains a failure. It never manufactures readiness or repeats a preparation
click. Production consent, histories, consumed markers, Room/backups and signing
remain unchanged, and no stored provider credential is read.

## Rollback

An ordinary reviewed feature-branch revert restores the earlier fixture helper,
without runtime or persisted-data conversion. Retain all assertions and gating
jobs; never reset owner journals or replay an uncertain request.

## Documentation updated

QA baseline and handoff distinguish the hosted missing-control RED, two focused
local passes and pending final-head gates. Existing ADR0010 test-only routing and
no-replay boundaries remain sufficient; no new durable decision is introduced.

## Follow-up

Run the required final committed-source Windows24-group/Python checks, real local
and hosted Android, Linux CI/HTTP gates, and a fresh verified debug APK. Publish
the actual final Actions ZIP URL and APK SHA256 on existing draft PR8. Owner
approval and Samsung acceptance remain separate; no merge or paid call is authorized.
