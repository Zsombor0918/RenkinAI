# Pin the offline HTTP fixture to a literal loopback address

## Summary

Correct only the JVM test fixture routing after actual Linux CI exposed canonical
hostname differences. The production diagnostic fix and Android route remain unchanged.

## Why

MockWebServer4.12 url() obtains InetAddress.canonicalHostName. Windows returned
127.0.0.1 in the successful local run, while Linux returned localhost. The fixture's
deliberate DNS allowlist permits only127.0.0.1, so Linux safely denied transport
before the local server. This is a test portability defect, not a provider error.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

Only `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaOfflineHarness.kt`
changes. New documentation retains the failure and follow-up verification contract.

## Behavior before / after

The test-owned routing interceptor now sets the URL host to literal127.0.0.1 after
obtaining the server port/path. The existing DNS, direct-proxy, socket-address and
network-host guards remain unchanged. No allowlist broadening, production endpoint,
credential, retry, timeout, assertion or persistence change occurs.

## Tests actually run

Exact a234cf38 Linux offline gate actually runs14 cases/8 failures/zero errors/skips:
all5 parser tests pass;8 of9 HTTP tests fail before their expected server response.
Original CI independently executes1005 tests with the same8 failures. Failure ZIP
artifact12027973613 is16475 bytes and its downloaded SHA256 matches GitHub metadata:
b77d28b541e7ed1eaff151b732a85353299ec51634d4bc9f35b14b231d9e240b.
Pinned MockWebServer bytecode confirms canonical hostname lookup; actual Android
evidence already records localhost for the same API. A234's clean-tree local
emulator4 and Python64 passes remain valid prior checkpoints. The changed test
route requires fresh final-commit full/HTTP/CI runs, reported directly on PR8.

## Not tested

This correction makes no live request and does not establish phone/API behavior,
token sufficiency, generated visual quality or installed-signature compatibility.
Earlier Windows results cannot substitute for the corrected final source's checks.

## Risks

The correction is test-only and retains fail-closed external-network denial. Every
HTTP request still uses a harmless literal credential and synthetic artwork. A
failed fixture cannot accept a style or authorize a repeat provider submission.

## Rollback

A reviewed feature-branch revert restores the failing fixture route; no production
or persisted data conversion is needed. Retain the gating job and original assertions.

## Documentation updated

Handoff and QA baseline distinguish the observed Linux RED from the successful
Windows checkpoint and actual Android process tests. Final source and artifact
receipts remain in the existing draft PR8 audit after actual execution.

## Follow-up

Run the corrected final-head24-group Windows matrix, complete Linux CI, offline
HTTP/Android workflow and64 Python guards. Retrieve only the successful final-head
APK artifact, independently verify its bytes/identity/signature and publish its
actual download URL/hash on PR8. Keep all paid requests and merge unauthorized.
