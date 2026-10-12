# Current implementation

Codex 03 starts from accepted main `aefeb1b69e7757c757c60ac2f00ce2125893e1e6`,
verified October 11, 2026. PR #3 is merged (20:21:28 UTC), owner Zsombor0918;
head `e8e04c433ca94a4e28b5716f33581aeeb963b0a4` is an ancestor and its tree is
identical to that merge. Owner authorization supplied acceptance; no independent
APPROVED review is claimed. Codex 03 works on `feat/project-ai-persistence-m3`.
Verified Codex 03 head 06e71532 passed 718/718, every isolated original/new group
and debug APK, including legacy-gallery compatibility. The final documentation/
diagnostic-removal commit is checked separately in PR #4's exact-head audit;
owner and physical-device acceptance remain pending.
669/669 belongs to accepted Codex 02, not this change. Room is now v19 with an
additive v18 migration. Manifest, dependencies, identities, signing/install and
original Android CI remain unchanged. See [QA](../qa/BASELINE.md).

The M1 Robolectric 4.14.1 WRAP_CONTENT dialog-width correction remains in the
compatible native SDK 33 / 320dp form harness; original identifiers, interactions
and isolated/combined/form workflow are retained. Historical failed and successful
runs remain in [QA baseline](../qa/BASELINE.md) and [correction record](../changes/2026-10-11-compose-dialog-regression-fix.md), not fresh M2 results.

## Product and implemented paths

RenkinAI is a personal Renkin fork that turns app/pack/uploaded icons into a real
installable icon-pack APK on the phone, with optional paid external AI generation.
Ordinary icon creation/build can work offline; AI requires the chosen provider.

| Implemented area | Source evidence and boundary |
| --- | --- |
| Existing app list/editor | [MainActivity](../../app/src/main/kotlin/dev/renkinProject/renkin/MainActivity.kt), MainViewModel and UI; launcher components from InstalledAppCatalog, including user profiles; keys are package/activity, not labels |
| Projects entry point | [ProjectHome](../../app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectHome.kt) reads the real profile flow; ID-keyed cards show name, description, saved unbuilt flag and active-session unsaved edits; default/empty/loading/unavailable/busy states are explicit |
| Create / extend / classic entry | Create confirms the real profile form and waits for completion, then opens all known launcher targets without a source requirement. Extend chooses an external source by package, then explicitly chooses/creates a target profile through the existing Save/Cancel controller, then inspects actual source coverage. Project setup actions return to session selections; the complete classic editor and AI1/AI2 remain available |
| Home/setup/editor navigation | [RenkinAiRoot](../../app/src/main/kotlin/dev/renkinProject/renkin/ui/project/RenkinAiRoot.kt) uses the Activity's existing VM; editor Back returns Home after search/dialog handlers, Home clean Back exits, dirty Back offers return to editor or explicit exit without saving; no implicit save/switch/build/request on navigation |
| Pack sources, modifications, import | ApplicationProvider, IconGenerationService, IconGenerator, UploadedImageStore; custom/base/rendered separation and existing SVG/vector/adaptive paths |
| Named profiles | ProfileManager, DbApplication, DataPreferences; Room `renkinPack` v19, default id 1; settings snapshots switch with the profile, source/provenance locks remain |
| Export and installation | IconPackBuildService, IconPackBuilder, PackKeystore, ApkInstaller; calendar/clock/adaptive/themed outputs, appfilter/appmap/theme_resources; build snapshot persists to its owning profile |
| Backup/share/watch | data/transfer and data/watch plus WatchWorker/AutoBackupWorker; format 4 adds project history/PNG assets to full backups and profile shares; legacy archives remain readable, device gallery/keystore boundaries remain |
| Single AI icon | [AiIconGenerationService](../../app/src/main/kotlin/dev/renkinProject/renkin/ai/AiIconGenerationService.kt), AiIconViewModel, IndividualOptions; original icon plus up to five reference files; PNG/alpha validation, 512 px gallery/draft persistence before preview; user accepts upload draft, then editor applies and Save/Build persists |
| Missing-icon batch | [AiBatchRunner](../../app/src/main/kotlin/dev/renkinProject/renkin/ai/AiBatchRunner.kt), AiBatchStore/ViewModel and GlobalOptions; searchable selection, app prompt, paid confirmation, serial calls, pause/cancel/manual resume and one-app retry |
| App instructions | AiPromptStore; DataStore keyed by hash of profile/component; empty inherits common style; 2000-character maximum |
| Provider/key settings | AiSettingsStore global settings; OpenAI and experimental Stability clients; AES-GCM credential ciphertext in noBackupFilesDir, Android Keystore aliases unchanged |

## Exact identity and build configuration

- Kotlin namespace/package: `dev.renkinProject.renkin`; display name: Renkin AI.
- Release app: `hu.zsombor.renkinai`; debug app: `hu.zsombor.renkinai.dev`.
- Generated default-profile pack: `${BuildConfig.APPLICATION_ID}.pack`; other
  profiles append `.p<profileId>`. Smali class package remains upstream.
- versionCode 56, versionName `2026.10.04-ai2`; JDK 17, Gradle 8.11.1,
  AGP 8.10.1; compileSdk 36, targetSdk 35, minSdk 23.
- App signing and generated-pack signing are different keys. Full Renkin backup
  carries the pack keystore/password; profile-only export does not. API keys and
  batch journals/draft markers are outside backups. See [architecture](../ARCHITECTURE.md).

## Known limitations and cautions

- Different-project/create requests offer Save or Cancel for dirty work. Success
  requires the profile load and baseline reset; repeated taps cannot enqueue a
  second insert. Process restoration reports interruption without replaying the
  request. Any committed profile stays visible by ID; a failed load can be reloaded.
  Profile creation and preference/ icon loading are not a cross-store transaction.
- Classic editor UI state is saved for Home round trips; icons remain
  in MainViewModel/ApplicationProvider. Session-only unsaved icons still do not
  survive process death without explicit save/build, as in the existing editor.
- On Editor, loading conceals classic content and blocks its taps while retaining
  Activity-result registrations across early profile-ID changes. Home does not
  keep a hidden editor. Profile form measurement is bounded to the dialog width;
  retry explicitly resets list loading rather than briefly reusing stale rows.
- Editor-entry versions use the VM SavedStateHandle, so a restored image/watch
  request is not lost before the shell consumes it. Intro dismissal distinguishes
  pending preference reads from an explicit Settings request to show the tour again.
- Watch requests/pending suggestions and VM-owned legacy prompts take priority over
  Home. Shared images open the editor and toast directs users to an app's Upload
  tab. The original URI instance-state key suppresses recreation imports; separately
  delivered shares can import the same URI again. Upload handling remains unchanged.
- Wide/dark/light/hinge and interaction tests use Robolectric/fake state; real
  TalkBack, Samsung/fold lifecycle, notification delivery and installer round trips
  still need the recorded device checks in QA.
- The existing **AI2 quick batch** definition of missing remains `createdIcon == null` and not locked. Fallbacks/custom icons make
  an app non-missing; it is not an audit of a source pack's appfilter coverage.
- Batch is a ViewModel coroutine plus AtomicFile journal, not an AI WorkManager
  or foreground service. Android can kill it. REQUESTING recovery without a saved
  matching result becomes FAILED/uncertain; no automatic rebilling retry occurs.
- Each successful batch step applies a custom base/rendered icon and saves the
  **whole active profile**, including earlier unsaved edits. Preview each result
  before building; batch acceptance is structural, not visual recognition.
- HTTP/network errors stop the batch. A cancelled/transient request may have been
  billed remotely. Stability uses one style reference and two provider calls;
  no live Stability/device verification is recorded for Codex 00.
- The hardcoded model list is configuration in source, not proof of current API
  availability. No provider account, pricing or live request is verified here.
- Theme Park/One UI/Flip cover-screen compatibility requires a real device.
  The historical manifest INTERNET comment predates AI and is incomplete; actual
  networking also includes OpenAiImageClient and StabilityImageClient.
- Source supports backup/migrations; this UI PR does not test an
  actual device upgrade or guarantee a reproducible CI debug signing identity.

## Codex 02 read-only preparation and limitations

[ProjectSetup](../../app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectSetup.kt)
and [TargetSelection](../../app/src/main/kotlin/dev/renkinProject/renkin/ui/project/TargetSelection.kt)
provide real search, stable component checkboxes, select all/recommended missing,
clear and counts. CREATE keeps manual/applied/fallback rows in the full launcher
inventory. EXTEND keeps source facts separate from local icons and locks. Selection
is UI preparation only, never input to quick batch or automatic generation. Primitive
selection/search/source context survives setup/editor/Home saved UI round trips;
explicit Save now stores a per-profile preparation snapshot in ProjectAiMetadata. Reopen always requires current catalog/source verification and an explicit user review; selection is never cost consent.

[IconCoverageAnalyzer](../../app/src/main/kotlin/dev/renkinProject/renkin/apk/IconCoverageAnalyzer.kt)
uses the existing repository/parser. RESOLVABLE requires exact item mapping and a
successful nontransparent 96 px draw; declared missing/failed draw is BROKEN; a proven
index without an item is UNMAPPED. Invalid/missing XML is UNKNOWN, package change or
uninstall is UNAVAILABLE. Calendar/clock uncertainty and user identity remain UNKNOWN.
Fallback styling does not count as an exact mapped icon. The existing parser and
legacy last-valid duplicate winner remain unchanged, including ignoring null
attributes after a valid item; a sole missing drawable is still declared broken.

Local custom/manual and other-applied icons suppress recommendations; fallback can
still need replacement. Locks disable selection. Source-covered icons permit explicit
manual override. AI origin is never inferred from the existing DB. Room has no user
handle in its key: duplicate or non-current-user components are one unsupported,
disabled row with warning, not fictitious separate targets. A future user-key migration
requires its own decision and compatibility work.

Reads run off the main thread, sequentially, with cancellation between targets,
Boolean-only per-scan draw caching and one small preview at a time. Results carry
package/version/update/revision/theme plus profile/component/local snapshot. Reloads,
rapid source changes and leaving setup cancel prior jobs; resume and selected-package install/remove/update broadcasts revalidate package
identity. The receiver exists only while setup is composed; it never refreshes icons
or watches launcher apps in the background. Theme changes also invalidate reads. Explicit rescan rereads only the selected source or the existing pack catalog.
It does not change primary preferences, refresh icons, build, install or call AI.
A slow pathological drawable can delay cancellation during its own synchronous draw;
96 px validation cannot certify animation, monochrome or OEM behavior.

## Codex 03 project style and assets

[ProjectStylePanel](../../app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStylePanel.kt)
is reachable through either Create/Extend target setup, including CREATE without a
source. A profile starts unset. Users enter a description and separate project
instruction, import 0–5 PNG references, reorder/remove them and explicitly Save.
Global style/references are copied only through the labelled user action; AI1/AI2
continue using AiSettingsStore and their existing service, not project style.
No recognition, demo, gallery, batch launch, build or network action exists here.

[ProjectAiRepository](../../app/src/main/kotlin/dev/renkinProject/renkin/data/project/ProjectAiRepository.kt)
uses the existing Room DB. Style revisions are immutable and monotonically numbered
per profile; equal saves are idempotent. Expected revision and the existing VM session
mutex prevent stale/wrong-owner saves. Only durable completion reports Saved. Drafts
use primitive saved UI state; Back offers Cancel/explicit discard. External editor
attention preempts the panel while retaining its profile-keyed draft for reopening.
An explicitly discarded draft drops only unlinked references; historical revisions
and variants pin their images. Profiles cascade their new rows; default id 1 remains.

Private `project_ai_assets/<profileId>/<sha256>.png` copies survive Upload deletion.
Bounded CRC-checked, decodable, visible PNGs precede Room writes through temp/fsync/
rename. Normal imports never overwrite files; explicit validated restore can repair
corruption. References may be opaque, generated gateway output still requires genuine
alpha. Limits: 4 MiB/image, 1–4096 px dimensions, 64 MiB/project; restore permits up to
two quotas transiently while retaining the previous files. Explicit GC removes only
files without retained rows, never Upload, history, variants or pending draft rows.

Target selections store mode/source, catalog/index fingerprints and exact component
keys. Source/app/lock/user uncertainty invalidates reuse; the user must review current
available targets. Save rechecks current ownership, catalog and source index. Saved
selection is not GenerationPlan and carries no paid permission. Source context changes
on explicit Save clear the previous incompatible selection. No user-handle migration.

Full backup and profile share carry style history, ordered references, functional
variant records and verified PNGs. Shares allocate a new profile and remap every
owner/path, with revalidation; no API credentials, AI2 journals or draft markers travel.
Old full restores clear new metadata. Staging validates the complete allowlisted ZIP
before mutation; project files finalize before the final transactional profile/icon/
metadata replacement. Legacy watch/prefs/gallery/keystore remain separate stores:
restore is not cross-store ACID. Old APKs cannot read v19; see rollback in the ADR.

[IconGenerationGateway](../../app/src/main/kotlin/dev/renkinProject/renkin/ai/IconGenerationGateway.kt)
has a real adapter reusing existing OpenAI/Stability clients and AiCredentialStore,
and offline fakes. It preserves original-first ordered references, output validation,
no retries and typed uncertain/cancelled outcomes. Stability requires exactly one
reference and two requests. The adapter is not yet the legacy single/batch route and
has no project UI consumer. No live API request was made. Variant records are a
repository contract, not a new staging workflow or inferred origin for old icons.

## Planned, not implemented

Codex 04 automatic analysis, 05 trial icons/approval, 06 staging gallery and 07 edits
remain separate tasks. Project style is saved, never automatically approved. No
GenerationPlan entity, generation confirmation, backend, login, credits, Billing,
background scheduling, source auto-sampling or Play publication is implemented.
Device acceptance and exact-final-head automated evidence remain separate in QA
and the PR audit.
Follow [ROADMAP](../ROADMAP.md) and [HANDOFF](HANDOFF.md).

## Storage/thread and live-context boundaries

The project repository executes file and synchronous legacy DAO work on IO. Its
per-existing-DB asset lease serializes import/restore with explicit GC; shared
profile creation stays on Room's transaction dispatcher. Failed grouped imports
clean only their newly finalized candidate paths and unlinked rows. Historical,
variant, pre-existing pending and unrelated unrecorded files are preserved.
Explicit UI removal/discard releases only unlinked references; restored draft
import hashes cover replaced references too.

Fresh launcher identity-only reads on composed preparation resume/package events
and explicit Save remove uninstalled components. New/unloaded or uncertain user
entries are disabled with an explanation until a real catalog reload verifies
local icons. Source-only observation stays the default for original M2 callers;
preparation explicitly opts into launcher changes. Source Save requests a fresh
read-only index matching the reviewed rescan fingerprint. No icon refresh,
primary-source mutation, hidden background watcher, network or paid permission.

Legacy full-archive gallery entries allow 20 MiB with genuine signature/CRC/end/
dimension/decode checks and may be transparent, matching existing sampled uploads.
Project references retain 4 MiB and reject empty artwork. The genuine format-3
large/transparent gallery restore fixture passed with byte-for-byte comparisons
at 06e71532; upload/render code is unchanged.
