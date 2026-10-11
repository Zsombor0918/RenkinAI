#!/usr/bin/env python3
"""One Conventional Commit validator for commit-msg and PR CI (stdlib only)."""
import argparse
import re
import subprocess
import sys
from pathlib import Path

TYPES = "feat|fix|refactor|docs|test|build|ci|chore|perf|revert"
SUBJECT = re.compile(rf"^(?:{TYPES})\([a-z0-9][a-z0-9._/-]*\)!?: [a-z][\x20-\x7e]+$")


def validate_subject(subject):
    if not SUBJECT.fullmatch(subject) or not 10 <= len(subject) <= 72:
        return "use type(scope): short English imperative summary (ASCII, max 72 characters)"
    if subject.endswith((".", " ")) or "  " in subject:
        return "avoid a trailing period, trailing space, or repeated spaces"
    return None


def git(root, *args):
    return subprocess.check_output(["git", "-C", str(root), *args], stderr=subprocess.PIPE)


def commit_ref(root, ref):
    if ref.startswith("-"):
        raise ValueError("a Git ref cannot start with '-' ")
    return git(root, "rev-parse", "--verify", ref + "^{commit}").decode().strip()


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=Path.cwd())
    parser.add_argument("--message-file", type=Path)
    parser.add_argument("--subject")
    parser.add_argument("--base")
    parser.add_argument("--head", default="HEAD")
    parser.add_argument("--event-file", type=Path, help="pull_request event JSON; validate its title")
    args = parser.parse_args(argv)
    subjects = []
    try:
        if args.message_file:
            # Git may append comments/diffs; only the actual subject is validated.
            lines = args.message_file.read_text(encoding="utf-8-sig").splitlines()
            subjects.append(("commit", lines[0] if lines else ""))
        if args.subject is not None:
            subjects.append(("subject", args.subject))
        if args.event_file:
            import json
            event = json.loads(args.event_file.read_text(encoding="utf-8"))
            subjects.append(("PR title", event["pull_request"]["title"]))
        if args.base:
            base, head = (commit_ref(args.repo, ref) for ref in (args.base, args.head))
            # Exclude existing base history, validate every new commit, including merges.
            for sha in git(args.repo, "rev-list", f"{base}..{head}").decode().splitlines():
                subject = git(args.repo, "show", "-s", "--format=%s", sha).decode().rstrip("\n")
                subjects.append((sha[:12], subject))
        if not (args.message_file or args.subject is not None or args.base or args.event_file):
            parser.error("provide --message-file, --subject, --base, or --event-file")
        errors = [(label, subject, validate_subject(subject)) for label, subject in subjects]
        errors = [value for value in errors if value[2]]
        for label, subject, error in errors:
            print(f"FAIL {label}: {subject!r}: {error}", file=sys.stderr)
        if errors:
            return 1
        print(f"PASS commit conventions ({len(subjects)} subjects)")
        return 0
    except (OSError, ValueError, KeyError, subprocess.CalledProcessError) as error:
        print(f"FAIL commit conventions: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
