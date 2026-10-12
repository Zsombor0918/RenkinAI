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
    "project-style-ui": [PACKAGE + "ProjectStyleComposeTest", PACKAGE + "ProjectSelectionComposeTest"],
    "project-data": ["dev.renkinProject.renkin.data.project.ProjectAiPersistenceTest",
                     "dev.renkinProject.renkin.data.project.ProjectAiMigrationTest",
                     "dev.renkinProject.renkin.data.transfer.ProjectAiBackupTest",
                     PACKAGE + "ProjectPreparationTest"],
    "generation-gateway": ["dev.renkinProject.renkin.ai.IconGenerationGatewayTest",
                           "dev.renkinProject.renkin.ai.AiImagePipelineTest",
                           "dev.renkinProject.renkin.ai.AiPersistenceTest",
                           "dev.renkinProject.renkin.ai.AiBatchRunnerTest"],
    "reference-sampler": ["dev.renkinProject.renkin.ai.style.ReferenceSamplerTest"],
    "analysis-schema-composer": ["dev.renkinProject.renkin.ai.style.StyleAnalysisSchemaTest",
                                  "dev.renkinProject.renkin.ai.style.PromptComposerTest"],
    "analysis-gateway": ["dev.renkinProject.renkin.ai.style.StyleAnalysisGatewayTest"],
    "analysis-journal": ["dev.renkinProject.renkin.ai.style.StyleAnalysisJournalTest"],
    "analysis-style-ui": [PACKAGE + "ProjectStyleAnalysisComposeTest"],
    "analysis-persistence-session": ["dev.renkinProject.renkin.data.project.ProjectAiAnalysisPersistenceTest",
                                      PACKAGE + "OwnedStyleAnalysisTest"],
    "trial-domain": ["dev.renkinProject.renkin.ai.trial.StyleTrialTargetSelectorTest",
                     "dev.renkinProject.renkin.ai.trial.StyleTrialPlanTest"],
    "trial-gateway": ["dev.renkinProject.renkin.ai.trial.StyleTrialGatewayTest"],
    "trial-journal": ["dev.renkinProject.renkin.ai.trial.StyleTrialJournalTest"],
    "trial-ui": [PACKAGE + "StyleTrialComposeTest"],
    "trial-persistence-session": ["dev.renkinProject.renkin.data.project.StyleTrialPersistenceTest",
                                  "dev.renkinProject.renkin.data.project.StyleTrialMigrationTest",
                                  PACKAGE + "OwnedStyleTrialsTest"],
    "full-suite": [],
}


def check_reports(reports, selectors, exit_code):
    """Require actual testcase evidence, exact declared counts and every selector."""
    counts = {key: 0 for key in ("tests", "failures", "errors", "skipped")}
    observed, malformed = set(), []
    for report in reports:
        try:
            suite = ET.parse(report).getroot()
            cases = suite.findall("testcase")
            actual = {"tests": len(cases), "failures": sum(case.find("failure") is not None for case in cases),
                      "errors": sum(case.find("error") is not None for case in cases),
                      "skipped": sum(case.find("skipped") is not None for case in cases)}
            if suite.tag != "testsuite" or any(int(suite.attrib.get(key, 0)) != value for key, value in actual.items()):
                raise ValueError("JUnit suite counts do not match testcase evidence")
            observed.add(suite.attrib["name"])
            observed.update(suite.attrib["name"] + "." + case.attrib["name"].split("[", 1)[0] for case in cases)
            for key in counts:
                counts[key] += actual[key]
        except (ET.ParseError, KeyError, ValueError, OSError):
            malformed.append(str(report))
    missing = [selector for selector in selectors if selector not in observed]
    passed = exit_code == 0 and counts["tests"] > 0 and not missing and not malformed and not any(counts[key] for key in ("failures", "errors", "skipped"))
    return {"exit": exit_code, "passed": passed, "missing_selectors": missing, "malformed_reports": malformed, **counts}


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    summary = {"head": head, "cases": {}}
    failed = False
    for name, selectors in CASES.items():
        destination = OUTPUT / name
        if destination.exists():
            shutil.rmtree(destination)
        destination.mkdir(exist_ok=True)
        tasks = ["compileDebugKotlin", "testDebugUnitTest", "assembleDebug"] if name == "full-suite" else ["testDebugUnitTest"]
        command = ["./gradlew", *tasks, "--console=plain"]
        for selector in selectors:
            command.extend(["--tests", selector])
        # Prior run XML must never substitute for a command which produced no results.
        for path in [ROOT / "app/build/test-results/testDebugUnitTest", ROOT / "app/build/reports/tests/testDebugUnitTest"]:
            if path.exists():
                shutil.rmtree(path)
        print(f"Running {name} on {head}", flush=True)
        with (destination / "gradle.log").open("w") as log:
            result = subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
        for source, target in [("app/build/test-results/testDebugUnitTest", "xml"),
                               ("app/build/reports/tests/testDebugUnitTest", "html")]:
            path = ROOT / source
            if path.exists():
                shutil.copytree(path, destination / target, dirs_exist_ok=True)
        reports = list((destination / "xml").glob("TEST-*.xml"))
        required = selectors if name != "full-suite" else sorted({selector for group in CASES.values() for selector in group})
        summary["cases"][name] = check_reports(reports, required, result.returncode)
        failed |= not summary["cases"][name]["passed"]
        print(f"{name}: {summary['cases'][name]}", flush=True)
    # Retain Room's real KSP export; never synthesize or hand-edit identity/schema JSON.
    schema = ROOT / "app/schemas/dev.renkinProject.renkin.data.RenkinPackDatabase/19.json"
    summary["room_schema_19_exported"] = schema.is_file()
    if schema.is_file():
        shutil.copytree(ROOT / "app/schemas", OUTPUT / "schemas", dirs_exist_ok=True)
    else:
        failed = True
    schema20 = ROOT / "app/schemas/dev.renkinProject.renkin.data.RenkinPackDatabase/20.json"
    summary["room_schema_20_exported"] = schema20.is_file()
    failed |= not schema20.is_file()
    schema21 = ROOT / "app/schemas/dev.renkinProject.renkin.data.RenkinPackDatabase/21.json"
    summary["room_schema_21_exported"] = schema21.is_file()
    failed |= not schema21.is_file()
    (OUTPUT / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    return int(failed)


if __name__ == "__main__":
    sys.exit(main())
