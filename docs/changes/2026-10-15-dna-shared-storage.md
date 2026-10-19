# PR8 cross-pack local preparation and safe diagnostics

## Summary

Investigate the owner's identical Quick/Standard free-scan failure across every
tested pack on the existing draft PR8. Baseline is clean local Windows worktree
feat/ux-flow-stabilization at f46115f10090f04651994e521f10e899871970fc.

## Why

The owner reports: "Local files could not be read. Check available storage, then
retry the scan. No request was sent." No new screenshot or phone trace is mounted.
That message is not evidence of low disk space: the controller maps every history
exception, including integrity/path validation, to storage. Two independent pack
readers sharing a profile/history and a fresh-profile control distinguish these
local boundaries. A trusted Context path alias is another explicitly tested
hypothesis; it is not assumed to be the observed Samsung path.

## Scope / changed files

Behavior impact: changed
Architecture impact: none
Milestone impact: none
Durable decision: none

- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysisTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaWorkflowTest.kt`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleDnaComposeTest.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaJournal.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysis.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectAnalysisBackend.kt`
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleDnaPanel.kt`
- `app/src/main/res/values/strings.xml`

The bounded scope is the DNA free-preparation/storage boundary and privacy-safe
error feedback. The existing shell, provider transport, paid consent, schema21,
backups, assets, saved revisions, targets, trials, consumed IDs, identities and
both signing-key paths must remain preserved. Later visual and cleanup stages
are not implemented.

## Behavior before / after

Baseline rejects aliases of the Context-owned no-backup directory at the first
history listing, even without history. It also labels malformed shared history as
storage for unrelated packs/modes. After actual red execution, only the trusted
parent is canonicalized, matching the Legacy journal; descendant link/containment
checks stay intact. Corrupt history remains blocked and byte-preserved, with specific
history feedback. Every local boundary now supplies an ephemeral fixed stage/category
code, never exception prose. Source reads no longer imply low disk space. Neither
local reproduction proves the exact original Samsung path/exception; owner retest
or its fixed diagnostic remains necessary, and this is not claimed fully fixed on phone.

## Tests actually run

Native Windows11/PowerShell, existing Temurin17.0.20.1+1, SDK36/build-tools35 and
Gradle8.11.1 caches are reused. Before runtime edits, two new controller tests compile
and fail2/2 with exactly storage (allpacks-red,165.908s,0 errors/skips). Both unchanged
assertions subsequently pass. allpacks-green-1 executes20/1,102.148s; its sole failure
is the new linked-directory guard under native Windows File canonical semantics,
consistent with the retained existing symlink failure. It is not suppressed or
patched around in production. allpacks-focused executes24/24,30.365s,0 errors/skips:
complete owned/DNA Compose classes plus alias/backup/orphan/consumed-ledger recovery.
Both modes, two readers/themes, existing saved history and a fresh Room profile,
no-key scans, private-prose redaction, I/O/permission faults and free retry are covered.
Exact commands, XML and logs remain in the outside-repository task evidence folder.
All23/unfiltered, Python, docs/hooks/schema and APK checks must run on the committed
final HEAD; their actual counts/links/hash are recorded in PR8 after commit, without
substituting a subset or inventing a self-containing hash. Read-only adb lists no device.

## Not tested

Physical Samsung/One UI source resources, real storage permissions, installed-data
upgrade and visual fidelity are NOT RUN. No phone will be connected for this task;
this does not block local Room/Robolectric/Compose, CI or artifact verification.

## Risks

Corrupt history cannot be ignored, deleted or repaired speculatively: consumed
request evidence and uncertain submissions must remain fail closed. Diagnostics
must contain only fixed stage/category codes, never paths, IDs, exception prose,
inventories, images, prompts or credentials. No live/paid request is authorized.

## Rollback

Use a compatible reviewed revert or forward repair on this feature branch while
retaining Room21, tagged readers, immutable history, consumed IDs and both keys.
No destructive downgrade, data reset or request replay is required.

## Documentation updated

This scoped note and index record the owner finding and red/green boundary. Current
state, architecture, handoff and QA/checklist describe trusted-root normalization,
preserved fail-closed history, ephemeral diagnostics and the unresolved physical
gate. No architecture/milestone/durable decision changes; historical failures stay.

## Follow-up

Finish the focused red/green checks, full retained matrix/governance and final-head
CI. Download and verify a fresh APK, publish its actual artifact link/hash on PR8
and leave the PR draft/open for the owner's two-pack/two-mode device retest.
