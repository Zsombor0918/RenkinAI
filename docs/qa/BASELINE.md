# Verification baseline — Codex 00

Audit date: October 11, 2026. Source baseline main:
`eb4daa5de82fab54e115363c073eb8981f2203d6`. Infrastructure branch:
`chore/agent-docs-governance`. Final head SHA is recorded in the PR audit.

## Fresh checks in this work

| Check | Observed result | Evidence boundary |
| --- | --- | --- |
| Git/main/PR audit | Clean initial main; no open PR; source paths read | Main HEAD and existing docs/workflows/build/manifest/license inspected |
| `python3 -m unittest discover -s scripts/tests -v` | 38 tests passed | Offline real Git fixtures; hook/CLI parity, bad/good commits/docs/links, index/head and merge-base behavior |
| `./gradlew testDebugUnitTest assembleDebug` | Attempt failed, exit 1 before Gradle configuration | Gradle 8.11.1 download: `java.net.SocketException: Network is unreachable` |
| Android environment | JDK 17.0.20 present; SDK not found in standard inspected locations | No usable cached Gradle distribution; no Android test count or APK produced here |
| Main protection reads | Inactive in observed APIs; no rulesets | Branch protected false; rulesets empty; direct protection read 403 integration restriction |
| [Governance run 38129900629](https://github.com/Zsombor0918/RenkinAI/actions/runs/38129900629) | Both jobs successful on initial PR head `dfe8d45b4287aeddf5ba73f95c67080ceb98832e` | Real PR commits/title/audit/docs + offline guard suite |
| [Android run 38129900648](https://github.com/Zsombor0918/RenkinAI/actions/runs/38129900648) | Failed before compile/tests/APK at SDK setup | v3 default `sdkmanager tools`: Failed to find package 'tools'; corrected by the follow-up note |

The final staged/range checks and PR CI outcomes are recorded in the change note
and PR. Only a completed workflow's observed result can be called a fresh CI pass.
Governance checks being present in source does not make them required on main.

## Historical evidence, not rerun

[AI_HANDOFF_HU](../AI_HANDOFF_HU.md) reports a successful October 4 build, 592
tests and a matching debug certificate. This is preserved historical author
evidence; Codex 00 does not re-attest that count, certificate or device behavior.
The initial main HEAD had no GitHub Actions run returned by the workflow-run query.

## Not exercised

No physical device/emulator, live OpenAI/Stability call, actual key decryption,
backup restore/upgrade, pack installation/update, Theme Park, One UI or Flip cover
screen was tested. No Play submission or administration write was made.
Use [TEST_MATRIX](TEST_MATRIX.md) for the remaining scenarios.
