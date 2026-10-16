# Offline Style DNA verification

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
python scripts/run_compose_regressions.py
python -m unittest discover -s scripts/tests -v
python scripts/run_offline_dna_emulator.py --serial emulator-5580
```

The complete regression runner retains the original23 groups and adds the offline
HTTP group. Nonzero exit, absent/malformed XML, mismatched testcase counts, missing
selectors, failures, errors and skips remain failures. Existing Windows AtomicFile,
symlink, locale and Compose-order assertions remain mandatory; compare actual
failures with an untouched baseline and Linux CI without hiding either result.

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
The `Offline Style DNA` workflow runs HTTP and Android jobs without
`continue-on-error`. This is a PR gate in the workflow, not a claim that repository
administration has made the new job names protected required contexts.

Actual executed selectors, observed failures/passes and exact final-head artifact
verification belong in the [PR8 audit](https://github.com/Zsombor0918/RenkinAI/pull/8).
If no real emulator run finishes, Android acceptance stays BLOCKED/NOT RUN;
Robolectric results do not replace it. Physical Samsung, installed signing-key
compatibility and live-provider/visual behavior remain separate owner checks.
