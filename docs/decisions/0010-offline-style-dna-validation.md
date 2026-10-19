# ADR0010 — production-path offline Style DNA validation

Date: October16,2026. Status: proposed for owner review.

## Context

Owner-device incomplete responses lost their reason field and previous fixture
success did not establish an offline Android end-to-end gate. No raw private phone
response is available, and no new paid request is authorized.

## Decision

Preserve the existing coordinator, Room21, immutable assets and consumed-ID
journals. Whitelist only the public incomplete reasons max_output_tokens and
content_filter into fixed safe diagnostic codes; missing, malformed and future
reasons use a fixed unknown code. Historical generic reasons remain readable.
Every incomplete result remains uncertain, accepts no style and cannot replay.
Do not change models, token limits, prices or endpoint counts from an unproven
phone-cause hypothesis.

Add test-owned HTTP routing and synthetic fixtures over the actual production
encoder/parser/controller/persistence. Independent network guards deny external
sockets; no production credential store is used. Instrumented Android tests use
an explicitly selected emulator and a separate harmless fixture APK. Release builds
have no fixture endpoint switch. Retain all existing mandatory regressions and add
failing HTTP/Android workflow jobs with durable evidence artifacts.

## Consequences and alternatives

Mocked Kotlin success returns alone cannot validate the network/persistence
contract and are insufficient for this gate. A paid smoke test would consume
resources without reproducible fault control and is not authorized. A new product
network layer, schema migration or UI refactor would duplicate established owners
and is rejected. Debug-only localhost policy and package visibility support the
instrumented fixture, while release identities, signing and provider URLs stay.

Automated fixture success does not prove token sufficiency, phone diagnosis,
provider quality, installed-app update compatibility or generated visual fidelity.
Owner review and merge remain separate; PR8 stays draft/open/unmerged.

## Scheduling amendment, October 17, 2026

[ADR0012](0012-lean-verification.md) supersedes the additive execution schedule only,
with explicit owner approval. All test cases and safety requirements remain;
HTTP executes in the authoritative full suite and Android remains a failing gate.
