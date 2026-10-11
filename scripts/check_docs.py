#!/usr/bin/env python3
"""Audit the Git index or PR snapshot, never unstaged documentation. Stdlib only."""
import argparse
import html
import json
import posixpath
import re
import subprocess
import sys
from pathlib import Path
from urllib.parse import unquote, urlsplit

REQUIRED_FILES = (
    "AGENTS.md", "README.md", "CONTRIBUTING.md", "CHANGELOG.md",
    ".github/PULL_REQUEST_TEMPLATE.md", ".github/workflows/ci.yml",
    ".github/workflows/governance.yml", ".githooks/pre-commit", ".githooks/commit-msg",
    "scripts/check_commit_messages.py", "scripts/check_docs.py", "scripts/tests/test_repo_guards.py",
    "docs/INDEX.md", "docs/ARCHITECTURE.md", "docs/AI_FORK_HU.md", "docs/AI_HANDOFF_HU.md",
    "docs/ROADMAP.md", "docs/agent/CURRENT_STATE.md", "docs/agent/WORKFLOW.md",
    "docs/agent/HANDOFF.md", "docs/changes/README.md",
    "docs/decisions/0001-repository-governance.md", "docs/qa/BASELINE.md",
    "docs/qa/TEST_MATRIX.md", "docs/policy/PLAY_RELEASE_GATE.md",
)
SECTIONS = ("Summary", "Why", "Scope / changed files", "Behavior before / after",
            "Tests actually run", "Not tested", "Risks", "Rollback",
            "Documentation updated", "Follow-up")
IMPACTS = {"Behavior impact": ("none", "changed"), "Architecture impact": ("none", "changed"),
           "Milestone impact": ("none", "changed"), "Durable decision": ("none", "recorded")}
PR_SECTIONS = ("Implemented behavior and scope", "Renkin regression risk", "Build and unit tests",
               "Manual device tests", "Database and profiles", "APK and installer",
               "AI requests and billing", "Documentation", "Rollback", "Known issues")
PLACEHOLDER = re.compile(r"(?im)(?:\b(?:TODO|TBD|FIXME|placeholder|fill (?:this|in)|replace me)\b|^\s*[-*> ]*\.\.\.\s*$|<insert[^>]*>)")
CHANGE_FILE = re.compile(r"^docs/changes/\d{4}-\d{2}-\d{2}-[a-z0-9]+(?:-[a-z0-9]+)*\.md$")
ADR_FILE = re.compile(r"^docs/decisions/\d{4}-[a-z0-9-]+\.md$")
SOURCE_ROOT = "app/src/main/kotlin/dev/renkinProject/renkin/"


def git(root, *args):
    return subprocess.check_output(["git", "-C", str(root), *args], stderr=subprocess.PIPE)


def resolve(root, ref):
    if ref.startswith("-"):
        raise ValueError("a Git ref cannot start with '-' ")
    return git(root, "rev-parse", "--verify", ref + "^{commit}").decode().strip()


class Snapshot:
    def __init__(self, root, ref=None):
        self.root, self.ref, self.cache = root, ref, {}
        command = ("ls-files", "--cached", "-z") if ref is None else ("ls-tree", "-r", "--name-only", "-z", ref)
        self.files = set(git(root, *command).decode().rstrip("\0").split("\0")) - {""}

    def read(self, path):
        if path not in self.cache:
            spec = f"{self.ref}:{path}" if self.ref else f":{path}"
            self.cache[path] = git(self.root, "show", spec).decode("utf-8")
        return self.cache[path]


def without_code(text, inline=True):
    text = re.sub(r"(?ms)^\s*(`{3,}|~{3,}).*?^\s*\1\s*$", "", text)
    text = re.sub(r"<!--.*?-->", "", text, flags=re.S)
    return re.sub(r"`[^`\n]*`", "", text) if inline else text


def section_bodies(text):
    matches = list(re.finditer(r"(?m)^##\s+(.+?)\s*$", text))
    return {m.group(1): text[m.end():matches[i + 1].start() if i + 1 < len(matches) else len(text)].strip()
            for i, m in enumerate(matches)}


def substantive(body):
    clean = re.sub(r"<!--.*?-->", "", body, flags=re.S)
    words = re.findall(r"[\w]+", clean)
    return len(words) >= 4 and len(clean.strip()) >= 20 and not PLACEHOLDER.search(clean)


def anchors(text):
    result, counts = set(), {}
    for heading in re.findall(r"(?m)^#{1,6}\s+(.+?)\s*#*\s*$", without_code(text, inline=False)):
        slug = html.unescape(re.sub(r"<[^>]+>", "", heading)).lower()
        slug = re.sub(r"[^\w\- ]", "", slug, flags=re.UNICODE).replace(" ", "-")
        count = counts.get(slug, 0)
        result.add(slug + (f"-{count}" if count else ""))
        counts[slug] = count + 1
    result.update(re.findall(r'''(?:id|name)=["']([^"']+)["']''', text))
    return result


def link_errors(snapshot):
    errors = []
    for path in sorted(p for p in snapshot.files if p.lower().endswith(".md")):
        text = without_code(snapshot.read(path))
        definitions = {m.group(1).casefold(): m.group(2) for m in
                       re.finditer(r"(?m)^\s*\[([^]]+)\]:\s*<?([^\s>]+)>?", text)}
        links = re.findall(r"\[[^]\n]*\]\(\s*(<[^>]+>|[^\s)]+)(?:\s+[\"'][^\n]*?[\"'])?\s*\)", text)
        links += re.findall(r'''(?:href|src)=["']([^"']+)["']''', text)
        links += list(definitions.values())
        for label, reference in re.findall(r"\[([^]\n]+)\]\[([^]\n]*)\]", text):
            key = (reference or label).casefold()
            if key not in definitions:
                errors.append(f"{path}: undefined link reference [{key}]")
        for target in links:
            url = urlsplit(html.unescape(target.strip("<>")))
            if url.scheme or url.netloc:
                continue  # External URLs are deliberately not fetched by this offline guard.
            link_path = unquote(url.path)
            destination = (posixpath.normpath(link_path.lstrip("/")) if link_path.startswith("/")
                           else posixpath.normpath(posixpath.join(posixpath.dirname(path), link_path)) if link_path else path)
            exists = destination in snapshot.files or any(p.startswith(destination + "/") for p in snapshot.files)
            if destination == ".." or destination.startswith("../") or not exists:
                errors.append(f"{path}: broken local link {target}")
            elif url.fragment and destination.lower().endswith(".md") and unquote(url.fragment) not in anchors(snapshot.read(destination)):
                errors.append(f"{path}: missing Markdown anchor {target}")
    return errors


def is_doc(path):
    return path.lower().endswith(".md") or path == "LICENSE" or (path.startswith("docs/") and path.endswith(".txt"))


def is_android_test(path):
    return path.startswith(("app/src/test/", "app/src/androidTest/"))


def required_for(paths):
    required = {"docs/agent/HANDOFF.md"}
    for path in paths:
        if path.startswith("app/src/main/") or path.startswith("app/schemas/"):
            required.add("docs/agent/CURRENT_STATE.md")
        if (path.startswith(tuple(SOURCE_ROOT + x for x in ("apk/", "data/", "di/", "ai/", "packages/", "service/")))
                or path in (SOURCE_ROOT + "MainActivity.kt", SOURCE_ROOT + "MainViewModel.kt", SOURCE_ROOT + "RenkinApplication.kt")
                or path in ("app/src/main/AndroidManifest.xml", "app/src/main/res/xml/manifest.xml")
                or path.startswith("app/src/main/assets/")):
            required.add("docs/ARCHITECTURE.md")
        if path.endswith((".gradle.kts", ".gradle")) or path.startswith("gradle/") or path in ("gradlew", "gradlew.bat", "gradle.properties", ".gitmodules"):
            required.update(("docs/agent/CURRENT_STATE.md", "docs/qa/BASELINE.md"))
        if path.startswith((".github/workflows/", ".githooks/", "scripts/")) or path in (".gitignore", ".gitattributes"):
            required.add("docs/agent/WORKFLOW.md")
        if path in ("scripts/check_docs.py", "scripts/check_commit_messages.py") or path.startswith(".githooks/") or path == ".github/workflows/governance.yml":
            required.add("@adr")
        if "ProjectHome" in path:
            required.add("docs/ROADMAP.md")
    return required


def audit(snapshot, previous, changed):
    errors = [f"required file missing: {p}" for p in REQUIRED_FILES if p not in snapshot.files]
    if errors:
        return errors
    errors += link_errors(snapshot)
    meaningful = {p for p in changed if not is_doc(p)}
    # Exempt changes need no note, but any note they DO add must still be complete.
    exempt = not meaningful or (len(meaningful) <= 3 and all(is_android_test(p) for p in meaningful))
    notes = [p for p in changed if CHANGE_FILE.fullmatch(p) and p in snapshot.files and p not in previous.files]
    if not notes and not exempt:
        return errors + ["meaningful change requires a NEW docs/changes/YYYY-MM-DD-short-topic.md entry"]
    required = set() if exempt else required_for(meaningful)
    covered = set()
    for note in sorted(notes):
        text = snapshot.read(note)
        bodies = section_bodies(text)
        headings = re.findall(r"(?m)^##\s+(.+?)\s*$", text)
        if any(headings.count(section) > 1 for section in SECTIONS):
            errors.append(f"{note}: duplicate audit section headings")
        from datetime import date
        try:
            date.fromisoformat(posixpath.basename(note)[:10])
        except ValueError:
            errors.append(f"{note}: invalid calendar date in filename")
        for section in SECTIONS:
            if not substantive(bodies.get(section, "")):
                errors.append(f"{note}: missing, empty, or placeholder section: {section}")
        scope = bodies.get("Scope / changed files", "")
        for field, choices in IMPACTS.items():
            match = re.search(rf"(?m)^{field}: ({'|'.join(choices)})$", scope)
            if not match:
                errors.append(f"{note}: declare {field}: {' / '.join(choices)} in Scope / changed files")
                continue
            if match.group(1) == choices[1]:
                required.add({"Behavior impact": "docs/agent/CURRENT_STATE.md", "Architecture impact": "docs/ARCHITECTURE.md",
                              "Milestone impact": "docs/ROADMAP.md", "Durable decision": "@adr"}[field])
        covered.update(p for p in meaningful if f"`{p}`" in scope)
    if not exempt:
        errors.extend(f"changed file not listed literally in change note Scope: {p}" for p in sorted(meaningful - covered))
    for path in sorted(required):
        if path == "@adr":
            if not any(ADR_FILE.fullmatch(p) and p in snapshot.files and substantive(snapshot.read(p)) for p in changed):
                errors.append("change requires an added/updated numbered docs/decisions ADR")
        elif path not in changed or path not in snapshot.files:
            errors.append(f"change requires updating {path}")
        elif path in previous.files:
            normalize = lambda text: re.sub(r"\s+", "", re.sub(r"<!--.*?-->", "", text, flags=re.S))
            if normalize(snapshot.read(path)) == normalize(previous.read(path)):
                errors.append(f"{path}: whitespace/comment-only updates do not satisfy the documentation requirement")
    return errors


def pr_audit_errors(event_file):
    body = json.loads(event_file.read_text(encoding="utf-8"))["pull_request"].get("body") or ""
    errors = []
    # Count all declaration candidates, including malformed/indented duplicates.
    declarations = [line for line in body.splitlines() if line.lstrip(" \t").startswith("Audit result:")]
    if len(declarations) != 1:
        errors.append("PR body requires exactly one Audit result: PASS / PASS WITH RISKS / BLOCKED declaration")
    else:
        status = re.fullmatch(r"Audit result: (PASS WITH RISKS|PASS|BLOCKED)[ \t]*", declarations[0])
        if not status:
            errors.append("PR body requires Audit result: PASS / PASS WITH RISKS / BLOCKED on its own unindented line")
        elif status.group(1) == "BLOCKED":
            errors.append("PR audit is BLOCKED; resolve blockers before merge")
    sections = section_bodies(body)
    for section in PR_SECTIONS:
        if not substantive(sections.get(section, "")):
            errors.append(f"PR audit requires a completed section: {section}")
    return errors


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=Path.cwd())
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--staged", action="store_true")
    mode.add_argument("--base", help="PR base ref; compare merge-base to --head")
    parser.add_argument("--head", default="HEAD")
    parser.add_argument("--event-file", type=Path, help="also validate PR audit body")
    args = parser.parse_args(argv)
    try:
        if args.staged:
            if git(args.repo, "ls-files", "--unmerged", "-z"):
                raise ValueError("resolve unmerged index entries before auditing")
            previous = Snapshot(args.repo, resolve(args.repo, "HEAD"))
            snapshot = Snapshot(args.repo)
            diff_args = ("diff", "--cached", "--no-renames", "--name-only", "-z", "HEAD", "--")
        else:
            base, head = (resolve(args.repo, ref) for ref in (args.base, args.head))
            merge_base = git(args.repo, "merge-base", base, head).decode().strip()
            previous, snapshot = Snapshot(args.repo, merge_base), Snapshot(args.repo, head)
            diff_args = ("diff", "--no-renames", "--name-only", "-z", merge_base, head, "--")
        changed = set(git(args.repo, *diff_args).decode().rstrip("\0").split("\0")) - {""}
        errors = audit(snapshot, previous, changed)
        if args.event_file:
            errors += pr_audit_errors(args.event_file)
        if errors:
            for error in errors:
                print(f"FAIL docs: {error}", file=sys.stderr)
            return 1
        print(f"PASS documentation audit ({len(changed)} changed paths; structural checks only)")
        return 0
    except (OSError, ValueError, KeyError, subprocess.CalledProcessError) as error:
        print(f"FAIL documentation audit: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
