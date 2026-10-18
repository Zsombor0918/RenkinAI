# Offline Style DNA verification

## Phase4A generation version check

The retained reference-first Android journey additionally checks V2 plan parse/hash equality after free Save and before any paid image consent. The native image-edit request and exact ordered PNG bytes are covered by local MockWebServer tests in the single authoritative unit suite. Existing Quick/Standard/incomplete/restart counts and the five-journey runner remain unchanged. No live endpoint or key is used.

## Phase3A additional free reference journey

The same emulator runner retains Quick/Standard/incomplete-cancel and restart,
adding referenceFirstJourney in the first instrumentation process (4+1 cases).
It samples actual installed fixture-pack resources, explicitly selects/reorders2,
saves the neutral brief without a key/analysis, prepares a genuine-original Trial
and requires zero HTTP POSTs. Trial preparation retains its existing key-presence
gate; only a synthetic boolean is supplied after the key-free Save. Restart also reads that saved manual revision and
its references, for3 saved projects total. Existing synthetic tokens are only
for the optional DNA journeys, never a real credential. No paid image request.


Origin tracing additionally emits trial.effect with changed key names (id/mode/source/reload/theme/available/network), trial.panel.attach/detach, invalidation.attach/detach/resume/package, and trial.reload.user. No package/profile/source values are emitted; package reason is a fixed action code plus source-match boolean. Correlate the existing monotonic seq/timestamp and backend/state/plan trace. The production CompositionLocal defaults to null; only the synthetic Trial subtree supplies a stable logger. Instrumentation can perturb timing; absence of a failing interleaving does not prove a fix.

## Standard diagnostic trace

The Android fixture emits OfflineDnaTrace JSON events; the runner saves tag-filtered logcat as trace-0.log and trace-1.log even when assertions fail. Compare ui.prepare.before/input.return, trial.prepare.dispatched/enter/return, trial.load/bind/release, trial.state.observed and scope.job.completed.after_finally. Save, selection, disk/evidence and journal filesystem events distinguish persisted style from journey completion. Each row carries process/thread, main-thread flag and monotonic time. StateFlow conflation and new-directory watcher attachment limit negative conclusions; do not infer an unobserved assignment never happened. The supported default Compose dispatcher remains unchanged.

These fixtures exercise the production request encoder, strict Responses parser,
owned controller, journals and Room. They never read a stored provider credential.
Synthetic PNGs and a generated, permission-free icon pack replace private artwork.
Mock results establish protocol and persistence behavior; they cannot establish
provider availability, actual token needs, cost or visual similarity.

## Windows setup and commands

Use JDK17, SDK36 and build-tools35 with the committed Gradle8.11.1 wrapper.
Initialize submodules and install hooks as described in [CONTRIBUTING](../../CONTRIBUTING.md).
The October16 new-PC bootstrap installs verified official portable tools under
the current user's LocalAppData; its process-scoped `env.ps1` supplies the paths.
SDK licenses were explicitly authorized. No administrator install, Windows feature
change, security bypass or reboot is required by this setup.

AGP rejects non-ASCII project paths on Windows. An ASCII directory junction to the
same checkout allows the path check to remain enabled. Invoke the scripts through
that alias; they preserve the supplied path. Configure sufficient local compiler
memory through the user's ignored Gradle properties, rather than changing project
requirements or suppressing a failed build. The first cold build can download the
wrapper, dependencies and Robolectric images; running the tests makes no paid call.

```powershell
. <current-user-tool-directory>\env.ps1
.\gradlew.bat testDebugUnitTest --tests '*StyleDnaOfflineHttpTest' --tests '*StyleDnaIncompleteReasonTest'
python scripts/run_compose_regressions.py --group offline-dna-http
python -m unittest discover -s scripts/tests -v
python scripts/run_offline_dna_emulator.py --serial emulator-5580
```

The authoritative full suite runs once in CI/build, including both HTTP classes.
Focused groups remain available for development; see [ADR0012](../decisions/0012-lean-verification.md).
Nonzero exit, absent/malformed XML, count mismatches, missing selectors, failures,
errors and skips still fail. Compare native Windows baseline failures separately.

## HTTP contract

`StyleDnaOfflineHttpTest` uses a disk SQLite database, closes and reopens Room,
reconstructs controller/journal readers and verifies immutable saved data and
consumed IDs. A test-only interceptor starts with the production HTTPS URL and
routes it to an IPv4 loopback MockWebServer. Independent DNS, proxy, network and
socket checks prevent external connections. The only token is a harmless literal.
No endpoint override is exposed in app settings, intents, release resources or
production dependency injection.

Quick12 requires one local POST and12 exact ID/hash-bound reports. Standard24
requires four six-image requests followed by one text synthesis. Before each POST,
the durable step marker must already exist. Explicit human review/Save creates the
immutable style revision. Reopen, changed context, cancellation, timeout, failed
parsing or partial progress cannot grant a replay or automatic continuation.

The matrix covers incomplete token-limit/filter/unknown reasons in both modes,
third-step failure retaining12 reports and three consumed steps, HTTP/auth/rate/
server failures, refusal, disconnect/timeout, invalid JSON/count/ID/hash, duplicate
keys, depth/body limits and cancellation. Verbose fixtures fill132 Quick observation
fields to their240-character bound;241 characters must fail. This measures the
client's structural capacity, not a model's required token budget. The configured
12288 output-token cap, model, detail and submission count remain unchanged.

Trial handoff checks the actual production image gateway/multipart order: genuine
synthetic target original first, then the reviewed frozen style-only references.
It checks bytes, membership and prompt constraints without grading artwork.

## Real Android gate

The runner requires an explicit `emulator-NNNN` serial and verifies
`ro.kernel.qemu=1` before installation. It never selects a connected phone. It builds
and installs only the debug app, instrumentation APK and `offline-fixture` APK on
that emulator. It records source SHA, dirty status, APK sizes/digests and device API.
Only the debug manifest allows the localhost fixture transport; release provider
endpoints and network policy remain unchanged.

Three instrumented journeys run first; the runner then explicitly stops the debug
app process and invokes the read-only restart selector in a new instrumentation
process. Logs and synthetic counters are retained in `build/offline-dna-emulator`.
The `Offline Style DNA` workflow runs the Android job without
`continue-on-error`. This is a PR gate in the workflow, not a claim that repository
administration has made the new job names protected required contexts.

Actual executed selectors, observed failures/passes and exact final-head artifact
verification belong in the [PR8 audit](https://github.com/Zsombor0918/RenkinAI/pull/8).
If no real emulator run finishes, Android acceptance stays BLOCKED/NOT RUN;
Robolectric results do not replace it. Physical Samsung, installed signing-key
compatibility and live-provider/visual behavior remain separate owner checks.
