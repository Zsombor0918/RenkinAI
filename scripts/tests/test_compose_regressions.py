"""Fault injection of the actual runner. Synthetic XML is not Android evidence."""
import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest import mock
import xml.etree.ElementTree as ET

SPEC = importlib.util.spec_from_file_location("compose_runner", Path(__file__).resolve().parents[1] / "run_compose_regressions.py")
RUNNER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(RUNNER)
SELECTOR = "dev.renkinProject.renkin.ui.project.ProjectStyleAnalysisComposeTest"


class ComposeRunnerFaultTests(unittest.TestCase):
    def test_native_windows_uses_batch_wrapper_and_posix_retains_shell_wrapper(self):
        with mock.patch.object(RUNNER.sys, "platform", "win32"):
            self.assertEqual(str(RUNNER.ROOT / "gradlew.bat"), RUNNER.gradle_wrapper())
        with mock.patch.object(RUNNER.sys, "platform", "linux"):
            self.assertEqual("./gradlew", RUNNER.gradle_wrapper())

    def execute(self, mode="pass"):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            schema = root / "app/schemas/dev.renkinProject.renkin.data.RenkinPackDatabase"
            schema.mkdir(parents=True)
            for version in (19, 20, 21):
                if not (version == 21 and mode == "missing_schema"):
                    (schema / f"{version}.json").write_text("synthetic test file; not a Room export")
            commands = []

            def gradle(command, **kwargs):
                commands.append(command)
                output = root / "app/build/test-results/testDebugUnitTest"
                output.mkdir(parents=True, exist_ok=True)
                # A successful command with no results must not reuse earlier run XML.
                if mode == "missing_xml" or (mode == "full_missing" and "assembleDebug" in command):
                    return subprocess.CompletedProcess(command, 0)
                name = "other.Test" if mode == "missing_selector" else SELECTOR
                counts = {key: "0" for key in ("failures", "errors", "skipped")}
                counts["tests"] = "0" if mode == "empty" else "1"
                tag = {"failure": "failure", "error": "error", "skipped": "skipped"}.get(mode)
                if tag:
                    counts[{"failure": "failures", "error": "errors", "skipped": "skipped"}[tag]] = "1"
                suite = ET.Element("testsuite", name=name, **counts)
                if mode != "empty":
                    case = ET.SubElement(suite, "testcase", name="actualInteraction")
                    if tag:
                        ET.SubElement(case, tag)
                if mode == "forged_counts":
                    suite.attrib["tests"] = "900"
                report = output / "TEST-synthetic.xml"
                if mode == "malformed":
                    report.write_text("<broken>")
                else:
                    ET.ElementTree(suite).write(report)
                return subprocess.CompletedProcess(command, 7 if mode == "nonzero" else 0)

            with mock.patch.object(RUNNER, "ROOT", root), mock.patch.object(RUNNER, "OUTPUT", root / "evidence"), \
                 mock.patch.object(RUNNER, "CASES", {"analysis-style-ui": [SELECTOR], "full-suite": []}), \
                 mock.patch.object(RUNNER.subprocess, "check_output", return_value="final-synthetic-head\n"), \
                 mock.patch.object(RUNNER.subprocess, "run", side_effect=gradle), \
                 mock.patch("builtins.print"):
                result = RUNNER.main()
            summary = json.loads((root / "evidence/summary.json").read_text())
            self.assertEqual("final-synthetic-head", summary["head"])
            wrapper = str(root / "gradlew.bat") if RUNNER.sys.platform == "win32" else "./gradlew"
            self.assertEqual([wrapper, "compileDebugKotlin", "testDebugUnitTest", "assembleDebug", "--console=plain"], commands[-1])
            return result, summary

    def test_new_required_selector_and_full_suite_pass_with_real_case_counts(self):
        code, summary = self.execute()
        self.assertEqual(0, code)
        self.assertTrue(all(case["passed"] and case["tests"] == 1 for case in summary["cases"].values()))

    def test_missing_selector_and_full_suite_missing_results_fail(self):
        for mode in ("missing_selector", "full_missing"):
            with self.subTest(mode=mode):
                code, summary = self.execute(mode)
                self.assertNotEqual(0, code)
                self.assertFalse(summary["cases"]["full-suite"]["passed"])

    def test_missing_and_empty_xml_fail(self):
        for mode in ("missing_xml", "empty"):
            with self.subTest(mode=mode):
                code, summary = self.execute(mode)
                self.assertNotEqual(0, code)
                self.assertEqual(0, summary["cases"]["analysis-style-ui"]["tests"])

    def test_skipped_failure_error_and_nonzero_exit_fail(self):
        for mode in ("skipped", "failure", "error", "nonzero"):
            with self.subTest(mode=mode):
                code, summary = self.execute(mode)
                self.assertNotEqual(0, code)
                self.assertFalse(summary["cases"]["analysis-style-ui"]["passed"])

    def test_malformed_xml_and_counts_without_testcase_evidence_fail(self):
        for mode in ("malformed", "forged_counts"):
            with self.subTest(mode=mode):
                code, summary = self.execute(mode)
                self.assertNotEqual(0, code)
                self.assertTrue(summary["cases"]["analysis-style-ui"]["malformed_reports"])

    def test_production_selectors_keep_original_cases_and_all_seven_analysis_groups(self):
        self.assertEqual([RUNNER.CREATION], RUNNER.CASES["creation"])
        self.assertEqual([RUNNER.INGRESS], RUNNER.CASES["ingress"])
        self.assertEqual([RUNNER.CREATION, RUNNER.INGRESS], RUNNER.CASES["both"])
        required = {"ReferenceSamplerTest", "StyleAnalysisSchemaTest", "PromptComposerTest", "StyleAnalysisGatewayTest",
                    "StyleAnalysisJournalTest", "ProjectStyleAnalysisComposeTest", "ProjectAiAnalysisPersistenceTest", "OwnedStyleAnalysisTest"}
        selectors = {selector.rsplit(".", 1)[-1] for group in RUNNER.CASES.values() for selector in group}
        self.assertTrue(required <= selectors)
        self.assertEqual([], RUNNER.CASES["full-suite"])

    def test_all_trial_selectors_are_additive_and_current_schema_is_required(self):
        required = {"StyleTrialTargetSelectorTest", "StyleTrialPlanTest", "StyleTrialGatewayTest",
                    "StyleTrialJournalTest", "StyleTrialComposeTest", "StyleTrialPersistenceTest",
                    "StyleTrialMigrationTest", "OwnedStyleTrialsTest"}
        selectors = {selector.rsplit(".", 1)[-1] for group in RUNNER.CASES.values() for selector in group}
        self.assertTrue(required <= selectors)
        _, summary = self.execute()
        self.assertTrue(summary["room_schema_21_exported"])
        code, summary = self.execute("missing_schema")
        self.assertNotEqual(0, code)
        self.assertFalse(summary["room_schema_21_exported"])

    def test_guided_journeys_add_to_all_retained_groups(self):
        self.assertEqual([RUNNER.PACKAGE + name for name in (
            "ProjectStyleGuidedFlowComposeTest", "GuidedTrialComposeTest",
            "ProjectSelectionComposeTest", "ProjectGuidedNavigationComposeTest",
            "GuidedProjectRoomJourneyComposeTest"
        )] + ["dev.renkinProject.renkin.testing.WindowsAtomicFileTest"], RUNNER.CASES["guided-ux"])
        self.assertEqual([RUNNER.PACKAGE + "StyleTrialComposeTest",
                          RUNNER.PACKAGE + "ProjectStyleTrialJourneyComposeTest"], RUNNER.CASES["trial-ui"])
        self.assertEqual([], RUNNER.CASES["full-suite"])


if __name__ == "__main__":
    unittest.main()
