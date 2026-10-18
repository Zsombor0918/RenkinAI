# Pin native montage HTTP fixture to literal loopback

## Summary

Correct the single demonstrated hosted Linux test failure after5779b096. Production
montage, analysis, V3 request, UI and saved-data behavior are unchanged.

## Why

MockWebServer can return localhost on Linux for a127.0.0.1 listener. The fixture
correctly rejects any host other than literal loopback, so its unnormalized image
endpoint was rejected before enqueue and no generated variant was produced.

## Scope / changed files

Behavior impact: none
Architecture impact: none
Milestone impact: none
Durable decision: none

`app/src/test/kotlin/dev/renkinProject/renkin/ai/style/PackStyleAnalysisTest.kt`

Only normalize the test endpoint host and assert the literal value. Retain the
existing socket/port checks, fake key, Proxy.NO_PROXY and no-redirect/no-retry client.

## Behavior before / after

The new actual-multipart fixture now addresses the exact listener consistently
across Windows/Linux, as the existing analysis and Android fixtures already do.
It still requires one saved variant, exact original/sheet bytes and order, one
reviewed prompt, separate consent, frozen hashes, source rejection and no replay.
No assertion, test, timeout or security check is removed or weakened.

## Tests actually run

Hosted CI39158605483 on5779b096 executes934:933 pass and this one fails at the
expected variant count. Its full XML/HTML/logs are retained with verified artifact
ZIP digest6e0e7bd47da0b5c18abdbd676a1d4d6b885b7806711c6e610e3f271931229802.
Corrected same native HTTP test passes1/1 on Windows in43s. Hosted Android39158605450
passes4/4 including real montage generation simulation/review and restart. Order
39158601142 passes7/7; hosted Python67/67 passes. All those are5779b096 evidence.

## Not tested

Corrected-head authoritative Linux/build, Android, order and APK verification follow
this commit. No paid/live API, owner key, phone or visual-quality trial is executed.
A passing Windows isolated test does not replace the failed Linux evidence.

## Risks

Historical merge-title91e5737d still blocks main-range governance. No history rewrite,
waiver or rules change is attempted. Final exact-head tests and certificate checks
must pass/be reported honestly before calling the debug artifact verified.

## Rollback

Use a normal reviewed revert if needed; no production or persisted format changes.
Never relax the literal-loopback/socket guard or retry paid requests to fix a test.

## Documentation updated

State, handoff and QA baseline distinguish the failed Linux source from successful
Android/order/Python evidence and the targeted fixture correction. Prior failures
remain preserved; final-head audit and artifact receipts are published on PR10.

## Follow-up

Push this minimal fixture correction for the final mandatory checks once. Do not run
another local full suite or begin another feature phase. Keep PR10 draft/open and
unmerged; owner still decides integration policy, signing/device acceptance and any
separately budgeted live visual pilot.
