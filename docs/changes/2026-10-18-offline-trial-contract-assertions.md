# Verify the active version at the offline Trial boundary

## Summary

Align one retained end-to-end HTTP test with the explicitly versioned new Trial
contract. Production code remains exactly as a383625c; no safety gate is weakened.

## Why

Authoritative Linux CI39118171451 on a383625c executes1026 cases:1025 PASS,1 FAIL.
StyleDnaOfflineHttpTest stops at line317 requiring old V1 Image1/STYLE REFERENCES
wording. Later assertions also require duplicated recipe boilerplate intentionally
replaced by the V2 authoritative contract. Hosted Android39118171487 passes5/5.
The failed Linux artifact12132798350 and its actual XML remain retained.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaOfflineHttpTest.kt`
asserts V2 identity/style roles, exact effective prompt hash, one appearance rule
set, unique drawing guidance and mandatory identity/redrawing/no-collage semantics.

## Behavior before / after

The old fixture expects V1 labels from newly prepared V2 plans. It now reads the
actual multipart prompt, verifies its frozen hash and semantic roles, keeping all
original byte/order/hash/source/member/consent/endpoint/restart/security assertions.
V1 exact golden-byte tests remain unchanged. No skip, retry, timeout or production
change is introduced; full Linux remains BLOCKED until a corrected-head pass.

## Tests actually run

Hosted RED1026/1 is independently downloaded and verified by artifact SHA256;
no result is called successful merely because the always-upload step ran. The
previous status update inferred success prematurely and was explicitly corrected.
Native Windows/JDK17 targeted StyleDnaOfflineHttpTest, StyleDnaIncompleteReasonTest and StyleTrialGatewayTest:31/31 PASS, zero errors/skips in2m54s including test recompilation. All14 retained HTTP cases pass. No complete local unit suite is repeated.

## Not tested

No paid/live API, phone, private signing key or installed-data access. No physical
Samsung visual fidelity or upgrade compatibility is inferred from synthetic tests.

## Risks

Prompt wording assertions can age when intentionally versioned contracts change.
Frozen-prompt hash and image identity checks provide stronger request evidence;
they still cannot prove a live model's aesthetic or brand-recognition quality.

## Rollback

Ordinary reviewed test follow-up revert restores the demonstrated false V1
expectation and therefore blocks new V2 acceptance. Do not conceal that failure,
change V1 golden bytes or erase consumed request markers to restore green checks.

## Documentation updated

CURRENT_STATE and HANDOFF record the precise failed-head evidence; TEST_MATRIX
and INDEX link this narrow fixture alignment. ADR0014 and production architecture
are unchanged. The final exact SHA/results belong externally in the same draft PR.

## Follow-up

After focused GREEN, commit this narrow correction and run one authoritative full
Linux/build plus the same five Android journeys on the corrected final head.
Then verify the APK and final governance, publish actual evidence and stop before
Phase4B or any paid visual pilot. Keep the PR draft/open/unmerged for owner review.
