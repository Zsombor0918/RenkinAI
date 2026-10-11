"""Offline integration tests: real Git commits/indexes plus actual hooks and CLI exits."""
import importlib.util
import json
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("check_docs", REPO / "scripts/check_docs.py")
docs = importlib.util.module_from_spec(spec)
spec.loader.exec_module(docs)
CODE = "app/src/main/kotlin/example/Screen.kt"


class RepoGuardsTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.git("init", "-b", "main")
        self.git("config", "user.name", "Guard Test")
        self.git("config", "user.email", "guard@example.invalid")
        self.git("config", "commit.gpgSign", "false")
        self.git("config", "core.autocrlf", "false")
        for path in docs.REQUIRED_FILES:
            source = REPO / path
            if path.startswith(("scripts/", ".githooks/", ".github/workflows/")) and source.exists():
                self.write(path, source.read_text(encoding="utf-8"))
            else:
                self.write(path, "# Fixture document\n\nInitial factual documentation for an offline test repository.\n")
        self.write(CODE, "fun screen() = 1\n")
        self.git("add", ".")
        self.git("commit", "-m", "Historical message is intentionally not conventional")
        self.base = self.git("rev-parse", "HEAD").stdout.strip()

    def git(self, *args, check=True):
        return subprocess.run(["git", "-C", str(self.root), *args], text=True, capture_output=True, check=check)

    def write(self, path, text):
        target = self.root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text, encoding="utf-8")

    def cli(self, script, *args):
        return subprocess.run([sys.executable, str(REPO / "scripts" / script), "--repo", str(self.root), *args],
                              cwd=self.root, text=True, capture_output=True)

    def check_staged(self):
        return self.cli("check_docs.py", "--staged")

    def stage(self):
        self.git("add", ".")

    def documented(self, paths=(CODE,), impacts=None):
        values = {k: v[0] for k, v in docs.IMPACTS.items()}
        values.update(impacts or {})
        blocks = []
        for name in docs.SECTIONS:
            body = "This change was inspected with an offline Git fixture and has an explicit review boundary."
            if name == "Scope / changed files":
                body = "\n".join(f"{key}: {value}" for key, value in values.items())
                body += "\n\n" + "\n".join(f"- `{path}`" for path in paths)
            blocks.append(f"## {name}\n\n{body}\n")
        self.write("docs/changes/2026-10-11-test-change.md", "# Test change\n\n" + "\n".join(blocks))
        required = docs.required_for(paths) | {"docs/agent/CURRENT_STATE.md"}
        for key, target in (("Behavior impact", "docs/agent/CURRENT_STATE.md"),
                            ("Architecture impact", "docs/ARCHITECTURE.md"),
                            ("Milestone impact", "docs/ROADMAP.md"), ("Durable decision", "@adr")):
            if values[key] == docs.IMPACTS[key][1]:
                required.add(target)
        for path in required:
            if path == "@adr":
                path = "docs/decisions/0002-test-decision.md"
            self.write(path, "# Verified update\n\nThe changed behavior and its source evidence were reviewed for this fixture.\n")

    def event(self, status="PASS WITH RISKS", title="chore(repo): establish audit gates"):
        body = f"Audit result: {status}\n\n" + "\n".join(
            f"## {name}\n\nReviewed in this fixture; device and Android build checks have not been run.\n" for name in docs.PR_SECTIONS)
        event = self.root / "event.json"
        event.write_text(json.dumps({"pull_request": {"title": title, "body": body}}), encoding="utf-8")
        return str(event)

    def test_valid_commit_subjects(self):
        for subject in ("chore(repo): establish audit gates", "feat(projects)!: add project home", "fix(batch): preserve interrupted state"):
            with self.subTest(subject=subject):
                self.assertEqual(self.cli("check_commit_messages.py", "--subject", subject).returncode, 0)

    def test_invalid_commit_subjects_exit_nonzero(self):
        for subject in ("bad message", "feat: add home", "fix(batch): Javitas", "fix(batch): add home.", "fix(batch): ", "feat(repo): " + "a" * 80):
            with self.subTest(subject=subject):
                self.assertNotEqual(self.cli("check_commit_messages.py", "--subject", subject).returncode, 0)

    def test_pr_range_excludes_historical_base_commits(self):
        self.write("README.md", "# Updated\n\nOnly current documentation changed in this fixture.\n")
        self.stage()
        self.git("commit", "-m", "docs(repo): clarify the baseline")
        self.assertEqual(self.cli("check_commit_messages.py", "--base", self.base).returncode, 0)

    def test_pr_range_checks_every_new_commit(self):
        self.write("README.md", "# One\n")
        self.stage()
        self.git("commit", "-m", "invalid new commit")
        self.write("README.md", "# Two\n")
        self.stage()
        self.git("commit", "-m", "docs(repo): improve the second note")
        self.assertNotEqual(self.cli("check_commit_messages.py", "--base", self.base).returncode, 0)

    def test_pr_title_is_checked_from_json(self):
        self.assertNotEqual(self.cli("check_commit_messages.py", "--event-file", self.event(title="No format")).returncode, 0)
        self.assertEqual(self.cli("check_commit_messages.py", "--event-file", self.event()).returncode, 0)

    def test_code_without_change_note_fails(self):
        self.write(CODE, "fun screen() = 2\n")
        self.stage()
        result = self.check_staged()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("NEW docs/changes", result.stderr)

    def test_empty_note_fails(self):
        self.write(CODE, "fun screen() = 2\n")
        self.documented()
        self.write("docs/changes/2026-10-11-test-change.md", "# Empty\n" + "\n".join(f"## {s}\n" for s in docs.SECTIONS))
        self.stage()
        self.assertNotEqual(self.check_staged().returncode, 0)

    def test_placeholder_note_fails(self):
        self.write(CODE, "fun screen() = 2\n")
        self.documented()
        note = self.root / "docs/changes/2026-10-11-test-change.md"
        note.write_text(note.read_text().replace("## Why\n", "## Why\n\nTODO fill this section with reasons\n"))
        self.stage()
        self.assertNotEqual(self.check_staged().returncode, 0)

    def test_documented_change_passes(self):
        self.write(CODE, "fun screen() = 2\n")
        self.documented()
        self.stage()
        result = self.check_staged()
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_unstaged_documents_cannot_satisfy_guard(self):
        self.write(CODE, "fun screen() = 2\n")
        self.git("add", CODE)
        self.documented()
        self.assertNotEqual(self.check_staged().returncode, 0)

    def test_index_snapshot_ignores_unstaged_broken_link(self):
        self.write("README.md", "# Valid\n\n[State](docs/agent/CURRENT_STATE.md)\n")
        self.git("add", "README.md")
        self.write("README.md", "[Broken](does-not-exist.md)\n")
        self.assertEqual(self.check_staged().returncode, 0)

    def test_broken_local_links_fail(self):
        for link in ("[Missing](missing.md)", '<img src="missing.png">', "[Ref][x]\n\n[x]: missing.md", "[Anchor](README.md#missing)", "[Escape](../outside.md)"):
            with self.subTest(link=link):
                self.write("README.md", "# README\n\n" + link + "\n")
                self.stage()
                self.assertNotEqual(self.check_staged().returncode, 0)

    def test_valid_links_anchors_references_and_code_examples(self):
        self.write("README.md", "# README\n\n[Self](#readme)\n[State][state]\n\n[state]: docs/agent/CURRENT_STATE.md\n\n```md\n[Example](missing.md)\n```\n`[Example](missing.md)`\n[Website](https://example.invalid/does-not-exist)\n")
        self.stage()
        self.assertEqual(self.check_staged().returncode, 0)

    def test_docs_only_exception(self):
        self.write("docs/ROADMAP.md", "# Plans\n\nOnly documentation changes; runtime stays identical.\n")
        self.stage()
        self.assertEqual(self.check_staged().returncode, 0)

    def test_docs_only_empty_change_note_still_fails(self):
        self.write("docs/changes/2026-10-11-empty.md", "# An empty note\n")
        self.stage()
        self.assertNotEqual(self.check_staged().returncode, 0)

    def test_duplicate_audit_heading_fails(self):
        self.write(CODE, "fun screen() = 2\n")
        self.documented()
        note = self.root / "docs/changes/2026-10-11-test-change.md"
        with note.open("a") as handle:
            handle.write("\n## Summary\n\nAnother duplicate summary creates an ambiguous audit record.\n")
        self.stage()
        self.assertNotEqual(self.check_staged().returncode, 0)

    def test_comment_only_required_doc_update_fails(self):
        self.write(CODE, "fun screen() = 2\n")
        self.documented()
        path = "docs/agent/CURRENT_STATE.md"
        self.write(path, self.git("show", f"HEAD:{path}").stdout + "\n<!-- invisible audit bypass -->\n")
        self.stage()
        self.assertNotEqual(self.check_staged().returncode, 0)

    def test_invalid_note_date_fails(self):
        self.write(CODE, "fun screen() = 2\n")
        self.documented()
        old = self.root / "docs/changes/2026-10-11-test-change.md"
        old.rename(old.with_name("2026-99-99-test-change.md"))
        self.stage()
        self.assertNotEqual(self.check_staged().returncode, 0)

    def test_inline_code_heading_anchor_passes(self):
        self.write("README.md", "# `Some code` heading\n\n[Self](#some-code-heading)\n")
        self.stage()
        self.assertEqual(self.check_staged().returncode, 0)

    def test_small_android_test_only_exception(self):
        self.write("app/src/test/ExampleTest.kt", "fun test() = Unit\n")
        self.stage()
        self.assertEqual(self.check_staged().returncode, 0)

    def test_large_test_change_needs_note(self):
        for i in range(4):
            self.write(f"app/src/test/Example{i}Test.kt", "fun test() = Unit\n")
        self.stage()
        self.assertNotEqual(self.check_staged().returncode, 0)

    def test_test_change_with_runtime_change_has_no_exception(self):
        self.write("app/src/test/ExampleTest.kt", "fun test() = Unit\n")
        self.write(CODE, "fun screen() = 2\n")
        self.stage()
        self.assertNotEqual(self.check_staged().returncode, 0)

    def test_deleted_code_also_needs_note(self):
        (self.root / CODE).unlink()
        self.stage()
        self.assertNotEqual(self.check_staged().returncode, 0)

    def test_rename_is_audited_as_delete_and_add(self):
        new = "app/src/main/kotlin/example/Renamed.kt"
        self.git("mv", CODE, new)
        self.assertNotEqual(self.check_staged().returncode, 0)
        self.documented((CODE, new))
        self.stage()
        self.assertEqual(self.check_staged().returncode, 0)

    def test_missing_current_state_fails(self):
        self.write(CODE, "fun screen() = 2\n")
        self.documented()
        self.git("checkout", "HEAD", "--", "docs/agent/CURRENT_STATE.md")
        self.stage()
        self.assertNotEqual(self.check_staged().returncode, 0)

    def test_architecture_path_requires_architecture_update(self):
        path = docs.SOURCE_ROOT + "apk/NewService.kt"
        self.write(path, "class NewService\n")
        self.documented((path,))
        self.git("checkout", "HEAD", "--", "docs/ARCHITECTURE.md")
        self.stage()
        self.assertNotEqual(self.check_staged().returncode, 0)

    def test_declared_milestone_requires_roadmap(self):
        self.write(CODE, "fun screen() = 2\n")
        self.documented(impacts={"Milestone impact": "changed"})
        self.git("checkout", "HEAD", "--", "docs/ROADMAP.md")
        self.stage()
        self.assertNotEqual(self.check_staged().returncode, 0)

    def test_guard_change_requires_workflow_and_adr(self):
        path = "scripts/check_docs.py"
        with (self.root / path).open("a") as handle:
            handle.write("\n# guard change\n")
        self.documented((path,))
        self.stage()
        self.assertEqual(self.check_staged().returncode, 0)
        (self.root / "docs/decisions/0002-test-decision.md").unlink()
        self.stage()
        self.assertNotEqual(self.check_staged().returncode, 0)

    def test_whitespace_only_required_doc_update_fails(self):
        self.write(CODE, "fun screen() = 2\n")
        self.documented()
        path = "docs/agent/CURRENT_STATE.md"
        old = self.git("show", f"HEAD:{path}").stdout
        self.write(path, old + "\n\n")
        self.stage()
        self.assertNotEqual(self.check_staged().returncode, 0)

    def test_existing_historical_note_cannot_be_reused(self):
        self.write("docs/changes/2026-10-11-test-change.md", "# Historical note\n")
        self.stage()
        self.git("commit", "-m", "docs(repo): record historical note")
        self.write(CODE, "fun screen() = 2\n")
        self.documented()
        self.stage()
        self.assertNotEqual(self.check_staged().returncode, 0)

    def test_changed_files_must_be_listed_in_scope(self):
        self.write(CODE, "fun screen() = 2\n")
        self.documented(("another-file.kt",))
        self.stage()
        self.assertNotEqual(self.check_staged().returncode, 0)

    def test_committed_head_uses_snapshot_not_worktree(self):
        self.write(CODE, "fun screen() = 2\n")
        self.documented()
        self.stage()
        self.git("commit", "-m", "fix(ui): update the screen")
        self.write("README.md", "[Unstaged broken link](missing.md)\n")
        result = self.cli("check_docs.py", "--base", self.base, "--head", "HEAD")
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_diverged_base_uses_merge_base(self):
        self.git("switch", "-c", "feature")
        self.write("README.md", "# Feature docs\n")
        self.stage()
        self.git("commit", "-m", "docs(repo): improve the readme")
        self.git("switch", "main")
        self.write("unrelated-script.py", "print('base only')\n")
        self.stage()
        self.git("commit", "-m", "chore(repo): add base-only script")
        later_base = self.git("rev-parse", "HEAD").stdout.strip()
        result = self.cli("check_docs.py", "--base", later_base, "--head", "feature")
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_pr_audit_blocked_and_empty_fail(self):
        self.assertNotEqual(self.cli("check_docs.py", "--staged", "--event-file", self.event(status="BLOCKED")).returncode, 0)
        event = self.root / "empty-event.json"
        event.write_text('{"pull_request": {"body": "Audit result: PASS"}}')
        self.assertNotEqual(self.cli("check_docs.py", "--staged", "--event-file", str(event)).returncode, 0)
        self.assertEqual(self.cli("check_docs.py", "--staged", "--event-file", self.event()).returncode, 0)

    def check_pr_audit(self, declarations, newline="\n"):
        event = Path(self.event())
        payload = json.loads(event.read_text(encoding="utf-8"))
        sections = payload["pull_request"]["body"].split("\n\n", 1)[1]
        payload["pull_request"]["body"] = (declarations + "\n\n" + sections).replace("\n", newline)
        event.write_text(json.dumps(payload), encoding="utf-8")
        # Use the committed diff/event arguments from documentation-audit in CI.
        return self.cli("check_docs.py", "--base", self.base, "--head", "HEAD", "--event-file", str(event))

    def test_pr_audit_accepts_exactly_one_nonblocked_declaration(self):
        for status in ("PASS", "PASS WITH RISKS"):
            for newline in ("\n", "\r\n"):
                for trailing in ("", " \t"):
                    with self.subTest(status=status, newline=newline, trailing=trailing):
                        result = self.check_pr_audit(f"Audit result: {status}{trailing}", newline)
                        self.assertEqual(result.returncode, 0, result.stderr)

    def test_pr_audit_rejects_a_single_blocked_declaration(self):
        result = self.check_pr_audit("Audit result: BLOCKED")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("PR audit is BLOCKED", result.stderr)

    def test_pr_audit_rejects_missing_declaration(self):
        result = self.check_pr_audit("")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("exactly one", result.stderr)

    def test_pr_audit_rejects_duplicate_declarations(self):
        for status in ("PASS", "PASS WITH RISKS", "BLOCKED"):
            with self.subTest(status=status):
                result = self.check_pr_audit(f"Audit result: {status}\nAudit result: {status}")
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("exactly one", result.stderr)

    def test_pr_audit_rejects_conflicting_declarations_in_any_order(self):
        for first in ("PASS", "PASS WITH RISKS", "BLOCKED"):
            for second in ("PASS", "PASS WITH RISKS", "BLOCKED"):
                if first != second:
                    with self.subTest(first=first, second=second):
                        result = self.check_pr_audit(f"Audit result: {first}\n\nAudit result: {second}")
                        self.assertNotEqual(result.returncode, 0)
                        self.assertIn("exactly one", result.stderr)

    def test_pr_audit_rejects_malformed_declarations(self):
        for declaration in ("Audit result:", "Audit result: UNKNOWN", "Audit result: pass",
                            "Audit result: PASS extra text", "Audit result:PASS", " Audit result: PASS"):
            with self.subTest(declaration=declaration):
                result = self.check_pr_audit(declaration)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("Audit result:", result.stderr)

    def test_pr_audit_valid_declaration_cannot_hide_a_malformed_duplicate(self):
        for extra in ("Audit result:", "Audit result: UNKNOWN", " Audit result: BLOCKED"):
            for declarations in (f"Audit result: PASS\n{extra}", f"{extra}\nAudit result: PASS"):
                with self.subTest(declarations=declarations):
                    result = self.check_pr_audit(declarations)
                    self.assertNotEqual(result.returncode, 0)
                    self.assertIn("exactly one", result.stderr)

    def test_real_commit_msg_hook_matches_cli(self):
        message = self.root / "message.txt"
        for subject, expected in (("fix(batch): preserve state", 0), ("bad commit", 1)):
            message.write_text(subject + "\n\nA longer body is allowed.\n")
            hook = subprocess.run(["sh", str(REPO / ".githooks/commit-msg"), str(message)], cwd=self.root, capture_output=True)
            cli = self.cli("check_commit_messages.py", "--message-file", str(message))
            self.assertEqual(hook.returncode, cli.returncode)
            self.assertEqual(hook.returncode, expected)

    def test_real_pre_commit_hook_matches_cli(self):
        self.write(CODE, "fun screen() = 2\n")
        self.stage()
        hook = subprocess.run(["sh", str(REPO / ".githooks/pre-commit")], cwd=self.root, capture_output=True)
        self.assertNotEqual(hook.returncode, 0)
        self.assertEqual(hook.returncode, self.check_staged().returncode)
        self.documented()
        self.stage()
        hook = subprocess.run(["sh", str(REPO / ".githooks/pre-commit")], cwd=self.root, capture_output=True)
        self.assertEqual(hook.returncode, 0, hook.stderr)

    def test_workflow_and_hooks_call_same_scripts_without_error_suppression(self):
        workflow = (REPO / ".github/workflows/governance.yml").read_text()
        for script in ("check_docs.py", "check_commit_messages.py"):
            self.assertIn("scripts/" + script, workflow)
            self.assertIn("scripts/" + script, "\n".join((REPO / ".githooks" / h).read_text() for h in ("pre-commit", "commit-msg")))
        self.assertIn("fetch-depth: 0", workflow)
        self.assertIn("contents: read", workflow)
        self.assertNotIn("continue-on-error", workflow)
        self.assertNotIn("|| true", workflow)
        self.assertNotIn("pull_request_target", workflow)

    def test_invalid_refs_exit_nonzero(self):
        self.assertNotEqual(self.cli("check_docs.py", "--base", "missing-ref").returncode, 0)
        self.assertNotEqual(self.cli("check_commit_messages.py", "--base", "missing-ref").returncode, 0)


if __name__ == "__main__":
    unittest.main()
