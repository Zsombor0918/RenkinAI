# Prevent an HTTP follow-up from replaying paid style analysis

## Summary

Make the dedicated analysis HTTP body one-shot so server retry hints cannot
resubmit a paid POST after its single durable consent claim.

## Why

Pinned OkHttp 4.12.0 RetryAndFollowUpInterceptor follows HTTP 503 with Retry-After: 0
even with retryOnConnectionFailure=false. The journal protects coordinator entries,
not retransmission inside one HTTP call. Upstream checks isOneShot before follow-up.

## Scope / changed files

Behavior impact: changed
Architecture impact: changed
Milestone impact: none
Durable decision: none

- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/OpenAiStyleAnalysisGateway.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisGatewayTest.kt`

This enforces the existing one-POST decision, not a new retry policy. Legacy image
clients, dependencies, credentials, endpoint/body fields and the journal stay intact.

## Behavior before / after

The analysis payload formerly used a repeatable buffered body. It now has bounded
UTF-8 bytes, exact content length/type and isOneShot=true. A 503 retry hint returns
the original uncertain result rather than silently sending another POST. The new
MockWebServer test contrasts the pinned client's unsafe repeatable-body control
with the real adapter; only mock requests occur.

## Tests actually run

Inspected upstream tag parent-4.12.0, commit 4984568367caaf359b82c452bd28b5e192824d1c,
RetryAndFollowUpInterceptor lines 118–121 and 255–267, plus RequestBody's contract.
Previous source head 780ab879 passed Android 38238659202 and regressions 38238659237:
135 XML, 791/791, all 16 groups, actual APK and byte-identical KSP v20. That result
does not prove this new HTTP test. Local 51 guards passed in 13.233 s at that head.

## Not tested

The new adapter/control requires fresh Android execution. No device, real endpoint,
account access, model quality or provider billing trial is run or authorized.

## Risks

A received 503 still has uncertain remote billing and needs a new explicit consent
for another attempt. One-shot transmission trades implicit recovery for the already
required spending boundary. No existing image-client behavior is changed here.

## Rollback

Do not restore a repeatable body without equivalent verified follow-up protection;
that would reopen the one-consent/two-POST gap. Database and signing are unchanged
by this correction; the overall v20 forward-repair rule still applies.

## Documentation updated

ARCHITECTURE, CURRENT_STATE and ADR 0005 describe the HTTP qualification. HANDOFF
and BASELINE retain the 791 checkpoint and pending new test without falsely
promoting old success into proof of this correction.

## Follow-up

Verify the negative control's two mock POSTs and adapter's single POST, all retained
groups, full suite/APK and final-head gates. Then complete the single PR audit and
leave owner merge/device acceptance separate; no live request is needed.
