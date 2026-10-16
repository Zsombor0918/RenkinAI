"""Fault injection for device safety and evidence; never invokes ADB or Gradle."""
import contextlib
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest import mock


SPEC = importlib.util.spec_from_file_location(
    "offline_emulator_runner", Path(__file__).resolve().parents[1] / "run_offline_dna_emulator.py")
RUNNER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(RUNNER)
HEAD = "a" * 40
EVIDENCE = {
    "quick": {"samples": 12, "reports": 12, "loopback_posts": 1, "external_posts": 0,
              "saved_revision": 1, "trial_prepared": True},
    "standard": {"samples": 24, "reports": 24, "loopback_posts": 5, "external_posts": 0,
                 "saved_revision": 1, "trial_prepared": True},
    "failure": {"loopback_posts": 2, "external_posts": 0, "consumed_attempts": 2,
                "accepted_styles": 0, "incomplete_reason": "RESPONSE_INCOMPLETE_MAX_OUTPUT_TOKENS",
                "cancel_reason": "CANCELLED_NO_REPLAY"},
    "restart": {"process_restart": True, "loopback_posts": 0, "external_posts": 0,
                "saved_projects_reopened": 2, "trial_prepared": True, "uncertain_attempts_replayed": 0},
}


class OfflineDnaEmulatorRunnerTests(unittest.TestCase):
    def test_physical_serial_is_rejected_before_environment_or_subprocess_access(self):
        with mock.patch.dict(os.environ, {}, clear=True), \
             mock.patch.object(RUNNER.sys, "argv", ["runner", "--serial", "physical-example"]), \
             mock.patch.object(RUNNER.subprocess, "run") as run, \
             mock.patch.object(RUNNER.subprocess, "check_output") as check_output, \
             contextlib.redirect_stderr(io.StringIO()) as error:
            with self.assertRaises(SystemExit) as failure:
                RUNNER.main()
        self.assertEqual(2, failure.exception.code)
        self.assertIn("Only an explicit emulator-NNNN serial is accepted", error.getvalue())
        run.assert_not_called()
        check_output.assert_not_called()

    def execute(self, mode="pass", evidence=None):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            commands, git_commands, apks = [], [], {}
            instrumentation_count = 0

            def completed(command, stdout="", returncode=0):
                return subprocess.CompletedProcess(command, returncode, stdout, "")

            def subprocess_run(command, **kwargs):
                nonlocal instrumentation_count
                commands.append((list(command), kwargs))
                if "assembleDebug" in " ".join(command):
                    if mode == "build_failure":
                        raise subprocess.CalledProcessError(1, command)
                    paths = ["offline-fixture/build/outputs/apk/debug/offline-fixture-debug.apk",
                             "app/build/outputs/apk/debug/app-debug.apk",
                             "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"]
                    if mode == "missing_apk":
                        paths.pop()
                    if mode == "extra_apk":
                        paths.append("app/build/outputs/apk/debug/stale-debug.apk")
                    for index, relative in enumerate(paths):
                        apk = root / relative
                        apk.parent.mkdir(parents=True, exist_ok=True)
                        payload = f"synthetic APK fixture {index}; not installable".encode()
                        apk.write_bytes(payload)
                        apks[relative] = payload
                    return completed(command)
                self.assertEqual(["-s", "emulator-5580"], command[1:3])
                self.assertGreater(kwargs["timeout"], 0)
                device = command[3:]
                if device == ["shell", "getprop", "ro.kernel.qemu"]:
                    return completed(command, "0\n" if mode == "not_emulator" else "1\n")
                if device[:2] == ["shell", "getprop"]:
                    values = {"ro.build.version.sdk": "35", "ro.product.model": "synthetic_emulator"}
                    return completed(command, values.get(device[-1], "synthetic") + "\n")
                if device[0] == "install":
                    if mode == "install_failure":
                        raise subprocess.CalledProcessError(1, command)
                    return completed(command, "Success\n")
                if device[:3] == ["shell", "am", "force-stop"]:
                    self.assertEqual(RUNNER.PACKAGE, device[-1])
                    return completed(command)
                if device[:3] == ["shell", "am", "instrument"]:
                    instrumentation_count += 1
                    if mode == "instrument_timeout":
                        raise subprocess.TimeoutExpired(command, kwargs["timeout"])
                    expected = 3 if instrumentation_count == 1 else 1
                    log = f"OK ({expected} tests)\n"
                    if instrumentation_count == 1:
                        if mode == "wrong_count":
                            log = "OK (1 test)\n"
                        if mode == "instrumentation_failed":
                            log += "INSTRUMENTATION_FAILED: synthetic failure\n"
                        if mode == "test_failure":
                            log += "FAILURES!!!\n"
                    return completed(command, log, 7 if mode == "instrument_nonzero" else 0)
                if device[:2] == ["shell", "run-as"]:
                    self.assertEqual([RUNNER.PACKAGE, "cat", "files/offline-dna-evidence.json"], device[2:])
                    if mode == "missing_evidence":
                        return completed(command, returncode=1)
                    payload = "{broken" if mode == "malformed_evidence" else json.dumps(
                        EVIDENCE if evidence is None else evidence)
                    return completed(command, payload)
                self.fail(f"Unexpected subprocess: {command}")

            def git_output(command, **kwargs):
                git_commands.append(command)
                if command == ["git", "status", "--porcelain"]:
                    return " M synthetic-source.kt\n" if mode == "dirty_source" else ""
                self.assertEqual(["git", "rev-parse", "HEAD"], command)
                revision_queries = sum(item == command for item in git_commands)
                return ("b" * 40 if mode == "head_changed" and revision_queries > 1 else HEAD) + "\n"

            failure, code = None, None
            with mock.patch.object(RUNNER, "ROOT", root), \
                 mock.patch.object(RUNNER.sys, "argv", ["runner", "--serial", "emulator-5580"]), \
                 mock.patch.dict(os.environ, {"ANDROID_HOME": str(root / "sdk"), "GITHUB_RUN_ID": "12345"}, clear=True), \
                 mock.patch.object(RUNNER.subprocess, "run", side_effect=subprocess_run), \
                 mock.patch.object(RUNNER.subprocess, "check_output", side_effect=git_output), \
                 mock.patch("builtins.print"):
                try:
                    code = RUNNER.main()
                except Exception as error:
                    failure = error
            summary_path = root / "build/offline-dna-emulator/summary.json"
            summary = json.loads(summary_path.read_text()) if summary_path.exists() else None
            return code, summary, commands, failure, apks

    def test_device_must_identify_as_emulator_before_build_install_or_force_stop(self):
        _, _, commands, failure, _ = self.execute("not_emulator")
        self.assertIsInstance(failure, RuntimeError)
        self.assertEqual(1, len(commands))
        self.assertEqual(["shell", "getprop", "ro.kernel.qemu"], commands[0][0][3:])

    def test_success_records_apk_hashes_source_and_workflow_provenance(self):
        code, summary, commands, failure, apks = self.execute()
        self.assertIsNone(failure)
        self.assertEqual(0, code)
        self.assertTrue(summary["passed"])
        self.assertEqual(HEAD, summary["head"])
        self.assertEqual("", summary["working_tree"])
        self.assertEqual("12345", summary["workflow_run"])
        self.assertEqual(EVIDENCE, summary["device_evidence"])
        recorded = {Path(item["path"]).as_posix(): item for item in summary["apks"]}
        self.assertEqual(set(apks), set(recorded))
        for path, payload in apks.items():
            self.assertEqual(len(payload), recorded[path]["size"])
            self.assertEqual(hashlib.sha256(payload).hexdigest(), recorded[path]["sha256"])
        build = next(index for index, (command, _) in enumerate(commands) if "assembleDebug" in " ".join(command))
        installs = [index for index, (command, _) in enumerate(commands) if command[3:4] == ["install"]]
        self.assertEqual(3, len(installs))
        self.assertTrue(all(build < index for index in installs))
        self.assertEqual([3, 1], [run["tests"] for run in summary["runs"]])
        stop = next(index for index, (command, _) in enumerate(commands) if command[3:6] == ["shell", "am", "force-stop"])
        phases = [index for index, (command, _) in enumerate(commands) if command[3:6] == ["shell", "am", "instrument"]]
        self.assertLess(phases[0], stop)
        self.assertLess(stop, phases[1])

    def test_build_install_timeout_and_malformed_evidence_cannot_report_pass(self):
        failures = {"build_failure": subprocess.CalledProcessError,
                    "install_failure": subprocess.CalledProcessError,
                    "instrument_timeout": subprocess.TimeoutExpired,
                    "malformed_evidence": json.JSONDecodeError}
        for mode, expected in failures.items():
            with self.subTest(mode=mode):
                _, summary, _, failure, _ = self.execute(mode)
                self.assertIsInstance(failure, expected)
                self.assertFalse(summary["passed"])
                self.assertEqual(type(failure).__name__, summary["error"])

    def test_missing_or_extra_apk_prevents_all_install_commands(self):
        for mode in ("missing_apk", "extra_apk"):
            with self.subTest(mode=mode):
                _, summary, commands, failure, _ = self.execute(mode)
                self.assertIsInstance(failure, RuntimeError)
                self.assertFalse(summary["passed"])
                self.assertFalse(any(command[3:4] == ["install"] for command, _ in commands))

    def test_misleading_ok_nonzero_exit_failure_markers_and_wrong_count_fail(self):
        for mode in ("instrument_nonzero", "instrumentation_failed", "test_failure", "wrong_count"):
            with self.subTest(mode=mode):
                code, summary, _, failure, _ = self.execute(mode)
                self.assertIsNone(failure)
                self.assertEqual(1, code)
                self.assertFalse(summary["passed"])
                self.assertFalse(summary["runs"][0]["passed"])

    def test_missing_device_evidence_and_head_drift_fail_even_with_all_tests_green(self):
        for mode in ("missing_evidence", "head_changed"):
            with self.subTest(mode=mode):
                code, summary, _, failure, _ = self.execute(mode)
                self.assertIsNone(failure)
                self.assertEqual(1, code)
                self.assertFalse(summary["passed"])
                self.assertTrue(all(run["passed"] for run in summary["runs"]))

    def test_external_posts_missing_journeys_wrong_counters_and_json_types_fail(self):
        mutations = [
            ("quick", "external_posts", 1),
            ("standard", "loopback_posts", 6),
            ("failure", "accepted_styles", 1),
            ("failure", "cancel_reason", "RETRY_ALLOWED"),
            ("restart", "uncertain_attempts_replayed", 1),
            ("quick", "loopback_posts", True),
            ("standard", "samples", 24.0),
            ("restart", "process_restart", 1),
            ("quick", "saved_revision", True),
            ("standard", "saved_revision", 0),
        ]
        invalid_evidence = []
        for section, key, value in mutations:
            evidence = json.loads(json.dumps(EVIDENCE))
            evidence[section][key] = value
            invalid_evidence.append((f"{section}.{key}={value!r}", evidence))
        missing = json.loads(json.dumps(EVIDENCE))
        del missing["restart"]
        invalid_evidence.extend([("missing restart", missing), ("array root", []), ("empty root", {})])
        for label, evidence in invalid_evidence:
            with self.subTest(evidence=label):
                code, summary, _, failure, _ = self.execute(evidence=evidence)
                self.assertIsNone(failure)
                self.assertEqual(1, code)
                self.assertFalse(summary["passed"])
                self.assertTrue(all(run["passed"] for run in summary["runs"]))

    def test_dirty_source_is_recorded_without_claiming_a_clean_commit(self):
        _, summary, _, _, _ = self.execute("dirty_source")
        self.assertEqual(" M synthetic-source.kt\n", summary["working_tree"])


if __name__ == "__main__":
    unittest.main()
