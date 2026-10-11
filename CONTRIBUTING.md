# Contributing to RenkinAI

Start with [AGENTS.md](AGENTS.md) and the [documentation index](docs/INDEX.md).
Contributors and AI agents use the same PR audit and shared guard scripts.

## Checkout and prerequisites

```sh
git clone --recurse-submodules https://github.com/Zsombor0918/RenkinAI.git
cd RenkinAI
git switch -c chore/your-topic
git config --local core.hooksPath .githooks
```

Use Python 3.10+ and Git for offline guards. Android builds additionally need JDK 17,
SDK Platform 36, Android build tools and dependency access; the Gradle wrapper pins
8.11.1 and the version catalog pins AGP 8.10.1. Android Studio can create the ignored
`local.properties`. Initialize the Arcticons-Font submodule before configuring Gradle.

## Windows and Git Bash

Install Git for Windows and Python 3, including Python on PATH (or the `py` launcher).
Run the checkout and hook configuration above in Git Bash; `core.hooksPath` is
repository-local and must be installed in each clone. Git executes the committed
shell hooks through its bundled shell even if a commit is started in PowerShell
or an IDE. LF line endings and executable Git file modes must be preserved.

```powershell
git config --local core.hooksPath .githooks
py -3 -m unittest discover -s scripts/tests -v
py -3 scripts/check_docs.py --staged
.\gradlew.bat testDebugUnitTest assembleDebug
```

If the hooks resolve another Python first, fix PATH to select Python 3. Check
`git config --get core.hooksPath`. Hooks are deliberately not installed by running
Gradle and can be skipped by Git; required server checks provide the merge gate.

## Commit and review

Allowed types: `feat`, `fix`, `refactor`, `docs`, `test`, `build`, `ci`, `chore`,
`perf`, `revert`. Scope is required; `!` is supported for a breaking change.

```text
chore(repo): establish documentation and audit gates
fix(batch): preserve interrupted generation state
docs(architecture): describe profile switching
```

Subject: English imperative, lowercase first word, ASCII, max 72 characters, no
trailing period. A body and Conventional Commit footers are allowed. Language and
imperative meaning require review; the script checks syntax, not English grammar.
No merge-message exemption exists: new merge commits on a PR branch need a valid
subject too. Prefer squash merge with a validated title; do not rewrite old history.

Stage code and the matching docs together. The pre-commit hook checks whitespace,
tracked local Markdown links and documentation in the **index**, not the working
tree. The commit-msg hook validates the message through the same Python script
used by CI. Run the final PR-range check before asking for review:

```sh
python3 -m unittest discover -s scripts/tests -v
python3 scripts/check_docs.py --base origin/main --head HEAD
python3 scripts/check_commit_messages.py --base origin/main --head HEAD
./gradlew compileDebugKotlin testDebugUnitTest assembleDebug
```

Complete every audit section and select `PASS`, `PASS WITH RISKS`, or `BLOCKED`.
Record device tests as not run when no device was used. The precise path rules and
small-change exceptions live in [WORKFLOW](docs/agent/WORKFLOW.md), not ad-hoc bypasses.
Main protection is an external setting; follow and verify the instructions there.
