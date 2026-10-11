#!/usr/bin/env python3
"""Retain real profile-form regressions separately and together; fail on any failure."""
import json
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "build" / "compose-regressions"
PACKAGE = "dev.renkinProject.renkin.ui.project."
CREATION = PACKAGE + "ProjectShellComposeTest.confirmedCreationRecompositionAndRestorationDoNotResubmitOrOpenEarly"
INGRESS = PACKAGE + "ProjectShellComposeTest.externalImageEntryAndPendingWatchTakePriorityOverHomeAndCreateForms"
CASES = {
    "creation": [CREATION],
    "ingress": [INGRESS],
    "both": [CREATION, INGRESS],
    "shared-form": [PACKAGE + "ProfileDetailsDialogComposeTest"],
    "create-extend": [PACKAGE + "CreateExtendFlowComposeTest"],
    "coverage-domain": ["dev.renkinProject.renkin.apk.IconCoverageAnalyzerTest",
                        "dev.renkinProject.renkin.apk.CoveragePreviewTest",
                        "dev.renkinProject.renkin.packages.CoverageAppFilterTest"],
    "full-suite": [],
}


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    summary = {"head": head, "cases": {}}
    failed = False
    for name, selectors in CASES.items():
        destination = OUTPUT / name
        destination.mkdir(exist_ok=True)
        tasks = ["compileDebugKotlin", "testDebugUnitTest", "assembleDebug"] if name == "full-suite" else ["testDebugUnitTest"]
        command = ["./gradlew", *tasks, "--console=plain"]
        for selector in selectors:
            command.extend(["--tests", selector])
        print(f"Running {name} on {head}", flush=True)
        with (destination / "gradle.log").open("w") as log:
            result = subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
        for source, target in [("app/build/test-results/testDebugUnitTest", "xml"),
                               ("app/build/reports/tests/testDebugUnitTest", "html")]:
            path = ROOT / source
            if path.exists():
                shutil.copytree(path, destination / target, dirs_exist_ok=True)
        reports = list((destination / "xml").glob("TEST-*.xml"))
        counts = {key: 0 for key in ("tests", "failures", "errors", "skipped")}
        observed = set()
        for report in reports:
            suite = ET.parse(report).getroot()
            attributes = suite.attrib
            observed.add(attributes["name"])
            observed.update(attributes["name"] + "." + case.attrib["name"].split("[", 1)[0]
                            for case in suite.findall("testcase"))
            for key in counts:
                counts[key] += int(attributes.get(key, 0))
        missing = [selector for selector in selectors if selector not in observed]
        passed = result.returncode == 0 and counts["tests"] > 0 and not missing and not any(counts[key] for key in ("failures", "errors", "skipped"))
        summary["cases"][name] = {"exit": result.returncode, "passed": passed, "missing_selectors": missing, **counts}
        failed |= not passed
        print(f"{name}: {summary['cases'][name]}", flush=True)
    (OUTPUT / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    return int(failed)


if __name__ == "__main__":
    sys.exit(main())
