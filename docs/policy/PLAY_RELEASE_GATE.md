# Future Play release gate

Status: **BLOCKED pending a separate evidence-based release audit**. Codex 00
does not publish, certify Google Play compliance or introduce public credits/billing.
The local user-entered API key remains the testing path in the current application.

## Evidence needed before a release decision

| Gate | Required record | Current evidence |
| --- | --- | --- |
| On-device APK generation/install | Trace actual manifest and generated APK, installer path/permissions; compare against then-current Play rules and Console declarations | Source exists; no release approval inferred |
| Target SDK and distribution | Verify current requirement against final build/catalog, signing and package identity | Source is target 35/compile 36; no claim about future eligibility |
| AI credentials and spending | Decide public backend/authentication, key custody, rate/spend controls, uncertain billing and explicit user consent | Personal keys encrypted locally; backend/spend enforcement not implemented |
| Paid credits and billing | Explicit product model and current platform/payment rules, test purchase/refund/failure paths | No public credit or billing implementation |
| Data handling | Actual network payloads, third-party services, backup contents, permissions, privacy policy and Console data declarations | Source audit describes local IDs and image payloads; legal declarations not approved |
| Licenses/attribution | GPLv3 distribution/source obligations, upstream attribution and third-party asset rights | [LICENSE](../../LICENSE) and upstream names retained; release-specific inventory still needed |
| Product/device validation | Current build/tests, migration/restore, install/update, launcher/Samsung compatibility | Outstanding device scenarios in [TEST_MATRIX](../qa/TEST_MATRIX.md) |

Review current official [Google Play policy center](https://play.google.com/about/developer-content-policy/)
and [Play Console help](https://support.google.com/googleplay/android-developer/) at
release time, cite exact dated sources and separate source facts from policy
interpretation. Preserve prior audits as historical evidence if imported later;
no prior external audit is treated as a verified current repository artifact here.

The release PR must record PASS/PASS WITH RISKS/BLOCKED, unresolved policy and
device issues, and an authorized owner's acceptance. Green build/governance CI
does not grant Play permission or waive this gate.
