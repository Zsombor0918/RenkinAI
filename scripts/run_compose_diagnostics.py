#!/usr/bin/env python3
"""Run independent real-dialog cases and retain each XML report, failing if any case fails."""
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "compose-diagnostics"
CASES = [
    "plainContent", "emptyDialog", "unlabelledFieldInDialog", "labelledFieldOutsideDialog",
    "labelledFieldInDialog", "supportingFieldInDialog", "threeFieldsInDialog",
    "realProfileDialog", "realProjectShell",
]


def main():
    OUTPUT.mkdir(exist_ok=True)
    failed = []
    for case in CASES:
        destination = OUTPUT / case
        destination.mkdir(exist_ok=True)
        command = ["./gradlew", "testDebugUnitTest", "--tests",
                   f"dev.renkinProject.renkin.ui.project.ProfileDialogDiagnosticsTest.{case}", "--console=plain"]
        print(f"Running {case}", flush=True)
        with (destination / "gradle.log").open("w") as log:
            result = subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
        for source, target in [("app/build/test-results/testDebugUnitTest", "xml"),
                               ("app/build/reports/tests/testDebugUnitTest", "html")]:
            path = ROOT / source
            if path.exists():
                shutil.copytree(path, destination / target, dirs_exist_ok=True)
        print(f"{case}: exit {result.returncode}", flush=True)
        if result.returncode != 0:
            failed.append(case)
    print(f"Failed cases: {', '.join(failed) or 'none'}", flush=True)
    return int(bool(failed))


if __name__ == "__main__":
    sys.exit(main())
