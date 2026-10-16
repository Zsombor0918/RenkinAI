# Offline Style DNA continuation on the new Windows PC

## Summary

Continue existing draft PR8 from fetched e235922b3b57961f21f0b91e47a8938f464705af.
Add safe incomplete-cause diagnostics and reusable production-path local HTTP,
SQLite/controller recovery and real Android emulator gates. Retain the completed
four-step UX and all earlier work; no paid provider call is authorized or needed.

## Why

The owner reports Quick0/12 and Standard0/24 incomplete results after submission.
The actual phone response is unavailable. The parser demonstrably discards the
documented incomplete_details.reason, so a generic error cannot identify the cause.
Prior mocks are insufficient evidence for network/persistence/Android behavior.

## Scope / changed files

Behavior impact: changed
Architecture impact: none
Milestone impact: none
Durable decision: recorded

The runtime change is restricted to diagnostic classification and explanatory text.
Test modules and workflow gates preserve the existing runtime architecture.

- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/OpenAiStyleDnaGateway.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleDnaPanel.kt`
- `app/src/main/res/values/ux_style_strings.xml`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaGatewayTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaIncompleteReasonTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaOfflineHarness.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaOfflineHttpTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleDnaComposeTest.kt`
- `app/src/androidTest/kotlin/dev/renkinProject/renkin/offline/StyleDnaEmulatorTest.kt`
- `app/src/debug/AndroidManifest.xml`
- `app/src/debug/res/xml/offline_test_network_security.xml`
- `app/build.gradle.kts`
- `settings.gradle.kts`
- `offline-fixture/build.gradle.kts`
- `offline-fixture/src/main/AndroidManifest.xml`
- `scripts/run_compose_regressions.py`
- `scripts/run_offline_dna_emulator.py`
- `scripts/tests/test_offline_dna_emulator.py`
- `.github/workflows/offline-dna.yml`
- `.github/workflows/ci.yml`

## Behavior before / after

Previously all incomplete statuses lost their reason. Fixed token-limit/filter/
unknown codes now retain safe distinctions; historical generic codes remain valid.
No raw output, provider prose or credential is retained. Status stays uncertain,
saved style remains intact and the original step stays consumed without replay.
Model/detail/12288-token budget/endpoint count and independent strict validation
remain unchanged. An ASCII invocation path and test-owned routing make the actual
HTTP/persistence contracts repeatable without a key. A separate fixture APK and
emulator-only runner add Android verification without touching the owner's phone.
The existing CI build retains every task and pins checkout to the actual PR head
so its downloadable APK has direct final-source provenance rather than relying
only on a PR merge-ref tree comparison.

## Tests actually run

New-PC inventory, official portable-download checksum verification, SDK setup and
WHPX/API35 emulator boot were observed. Original governance tests first exposed
missing Git shell and a Windows Python alias; after process-path correction,
`python -m unittest discover -s scripts/tests -v` passed55/55. The new runner's
nine fault-injection tests passed independently. Initial Gradle attempts failed
before assertions at non-ASCII path, compiler heap and worker-cache path boundaries;
their logs remain separate. Source-baseline and targeted RED/final-head executions
are retained outside Git and reported with actual XML counts in the PR8 audit.
Historical990-test evidence is not a fresh result for this continuation.
With the real ASCII Gradle home, the targeted six-test RED run reaches assertions:
six tests/four failures/zero errors/skips, consisting of three lost-reason parser
assertions and one missing fixed Compose explanation. Strict JSON bounds and old/
new schema1 record round-trips pass. Runtime edits follow that observed RED;
external red-xml, incomplete-red-result.json and the full log retain the evidence.
The first corrected run passes36/36 tests (gateway11, incomplete5, new HTTP9,
retained/additive Compose11), zero failures/errors/skips, and compiles Android
instrumentation in1m7s. An initial Android compile correctly exposed a missing
test-only import; correcting that import leaves runtime assertions unchanged.
Untouched baseline Windows full XML is990 tests/six failures/zero errors/skips;
the six retained filesystem/locale failures and separate passing debug assembly
are recorded externally. This focused GREEN does not replace final all-group CI.
Real API35/x86_64 Android instrumentation then passes3+1 journeys: Quick12/POST1,
Standard24/POST5, incomplete/cancel/POST2 with no style, and a fresh-process reopen
with POST0/no replay. Recorded process IDs4116→4271 prove the process changed;
both successful styles reopen and trial preparation succeeds. Emulator37.2.12
uses existing WHPX on this PC. The first three Android journeys failed safely at
transport because MockWebServer canonically resolved127.0.0.1 as localhost; actual
successful evidence records localhost. Test-only explicit127 routing corrects this
without relaxing DNS, network policy, assertions or timeouts. Both failed and
successful logs are retained outside Git. All64 Python guards now pass51.114s;
staged documentation audit passes32 changed paths. Final committed-source reruns
and the real downloadable artifact remain separate gates recorded on PR8.

The official [Responses reference](https://developers.openai.com/api/reference/resources/responses/methods/create)
and [reasoning guide](https://developers.openai.com/api/docs/guides/reasoning) were
checked October16. They establish incomplete metadata and token-limit semantics,
not the unavailable phone response or a justified change to this configured budget.

## Not tested

No live or paid API call, Samsung/Flip8 behavior, installed-data upgrade, release
signer comparison or generated-artwork fidelity was tested. Synthetic verbosity
does not prove actual model-token sufficiency. Emulator acceptance requires actual
instrumented completion; compilation or Robolectric alone cannot establish it.

## Risks

Public incomplete metadata identifies only a reported reason, not a verified phone
cause or exact bill. Native Windows filesystem/locale/order behavior can differ
from Android/Linux; retained failures must be compared with the untouched baseline.
Debug-only localhost policy has no release endpoint switch. Consumed/uncertain
markers, immutable histories, profiles/backups and both signing-key paths remain.

## Rollback

Use an ordinary reviewed feature-branch revert retaining compatible DNA readers,
Room21, immutable assets/history and consumed IDs. Never downgrade the database,
delete journals, replay an uncertain request, change signing keys or clear phone data.
Removing test modules/gates needs review; never weaken a failing assertion to pass.

## Documentation updated

Canonical current state, architecture, workflow, QA baseline/matrix and handoff
describe the narrow runtime behavior, actual environment and acceptance limits.
ADR0010 records test isolation/no-replay decisions. The documentation index,
contribution commands and changelog link the reusable offline workflow.

## Follow-up

Finish final-head Windows/HTTP/emulator/Python/CI checks and independently verify
the fresh debug artifact's package, certificate and SHA256 directly on PR8. Keep
the draft open/unmerged with its existing base. Physical/provider quality remains
a separate owner decision; this continuation grants no paid-request permission.
