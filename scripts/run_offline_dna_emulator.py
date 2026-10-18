#!/usr/bin/env python3
"""Run active synthetic reference/Trial journeys only on an explicitly selected emulator."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys

# Keep an ASCII junction supplied by Windows callers instead of resolving it back
# into an accented user-profile path rejected by AGP.
ROOT = Path(__file__).absolute().parents[1]
PACKAGE = "hu.zsombor.renkinai.dev"
TEST_CLASS = "dev.renkinProject.renkin.offline.StyleDnaEmulatorTest"


def valid_device_evidence(evidence):
    expected = {
        "references": {"loopback_posts": 0, "external_posts": 0, "selected_references": 2, "trial_prepared": True,
                       "declined": False, "generation_calls": 0},
        "decline": {"loopback_posts": 0, "external_posts": 0, "selected_references": 2, "trial_prepared": True,
                    "declined": True, "generation_calls": 0},
        "montage": {"loopback_posts": 2, "external_posts": 0, "montage_pages": 2, "trial_prepared": True,
                    "prompt_version": 3, "generation_calls": 1, "automatic_recomposition": True,
                    "variant_reviewed": True, "style_approved": True},
        "restart": {"process_restart": True, "loopback_posts": 0, "external_posts": 0,
                    "saved_projects_reopened": 3, "trial_prepared": True, "uncertain_attempts_replayed": 0},
    }
    if not isinstance(evidence, dict):
        return False
    for name, fields in expected.items():
        actual = evidence.get(name)
        if not isinstance(actual, dict) or any(type(actual.get(key)) is not type(value) or actual[key] != value
                                               for key, value in fields.items()):
            return False
    return all(type(evidence[name].get("saved_revision")) is int and evidence[name]["saved_revision"] >= 1
               for name in ("references", "decline", "montage")) and type(evidence["montage"].get("selected_icons")) is int and 19 <= evidence["montage"]["selected_icons"] <= 29


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True)
    args = parser.parse_args()
    if not re.fullmatch(r"emulator-\d+", args.serial):
        parser.error("Only an explicit emulator-NNNN serial is accepted")
    output = ROOT / "build/offline-dna-emulator"
    output.mkdir(parents=True, exist_ok=True)
    adb = str(Path(os.environ["ANDROID_HOME"]) / "platform-tools" / ("adb.exe" if sys.platform == "win32" else "adb"))

    def device(*command, check=True, timeout=60):
        return subprocess.run([adb, "-s", args.serial, *command], cwd=ROOT, text=True,
                              encoding="utf-8", errors="replace", capture_output=True, check=check, timeout=timeout)

    if device("shell", "getprop", "ro.kernel.qemu").stdout.strip() != "1":
        raise RuntimeError("Selected device did not identify itself as an emulator")
    summary = {"head": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
               "serial": args.serial, "api": device("shell", "getprop", "ro.build.version.sdk").stdout.strip(),
               "model": device("shell", "getprop", "ro.product.model").stdout.strip(), "runs": [], "passed": False,
               "working_tree": subprocess.check_output(["git", "status", "--porcelain"], cwd=ROOT, text=True),
               "workflow_run": os.environ.get("GITHUB_RUN_ID")}
    failed = True
    try:
        wrapper = str(ROOT / "gradlew.bat") if sys.platform == "win32" else "./gradlew"
        with (output / "build.log").open("w", encoding="utf-8") as log:
            subprocess.run([wrapper, ":app:assembleDebug", ":app:assembleDebugAndroidTest",
                            ":offline-fixture:assembleDebug", "--console=plain"], cwd=ROOT,
                           stdout=log, stderr=subprocess.STDOUT, check=True)
        paths = [ROOT / "offline-fixture/build/outputs/apk/debug/offline-fixture-debug.apk",
                 *sorted((ROOT / "app/build/outputs/apk/debug").glob("*.apk")),
                 *sorted((ROOT / "app/build/outputs/apk/androidTest/debug").glob("*.apk"))]
        if len(paths) != 3 or any(not path.is_file() for path in paths):
            raise RuntimeError("Expected exactly the fixture, app and instrumentation APK")
        summary["apks"] = [{"path": str(path.relative_to(ROOT)), "size": path.stat().st_size,
                            "sha256": hashlib.sha256(path.read_bytes()).hexdigest()} for path in paths]
        for path in paths:
            device("install", "-r", str(path))
        failed = False
        selectors = [",".join(TEST_CLASS + "#" + method for method in
                               ["referenceFirstJourney", "decliningTrialCannotRestoreTheFrozenPlan", "montageJourney"]),
                     TEST_CLASS + "#restartReadOnly"]
        for index, selector in enumerate(selectors):
            if index == 1:
                # Terminates the target/test process between writes and the read-only gate.
                device("shell", "am", "force-stop", PACKAGE)
            result = device("shell", "am", "instrument", "-w", "-r", "-e", "class", selector,
                            PACKAGE + ".test/androidx.test.runner.AndroidJUnitRunner", check=False, timeout=600)
            log = result.stdout + result.stderr
            trace = device("logcat", "-d", "-v", "threadtime", "OfflineDnaTrace:I", "*:S", check=False)
            (output / f"trace-{index}.log").write_text(trace.stdout + trace.stderr, encoding="utf-8")
            (output / f"instrumentation-{index}.log").write_text(log, encoding="utf-8")
            expected = 3 if index == 0 else 1
            passed = result.returncode == 0 and re.search(rf"OK \({expected} tests?\)", log) is not None
            passed &= "FAILURES!!!" not in log and "INSTRUMENTATION_FAILED" not in log
            summary["runs"].append({"selector": selector, "exit": result.returncode, "tests": expected,
                                     "passed": passed})
            failed |= not passed
            print(f"Instrumentation phase {index}: {'PASS' if passed else 'FAIL'}", flush=True)
        evidence = device("shell", "run-as", PACKAGE, "cat", "files/offline-dna-evidence.json", check=False)
        if evidence.returncode != 0:
            failed = True
        else:
            # Contains synthetic counters only; fixture code must never write private data.
            (output / "device-evidence.json").write_text(evidence.stdout, encoding="utf-8")
            summary["device_evidence"] = json.loads(evidence.stdout)
            failed |= not valid_device_evidence(summary["device_evidence"])
        final_head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
        failed |= final_head != summary["head"]
    except Exception as failure:
        failed = True
        summary["error"] = type(failure).__name__
        raise
    finally:
        summary["passed"] = not failed
        (output / "summary.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    return int(failed)


if __name__ == "__main__":
    sys.exit(main())
