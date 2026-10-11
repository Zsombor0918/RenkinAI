# Current implementation

Codex 01 source baseline: merged main `005cffa6d51b11e15c20e68349c0c4ce0f23cd8f`,
inspected October 11, 2026. PR #1 merged at 12:15:29 UTC with the P1 audit-result
fix and resolved review thread. Codex 01 adds the Projects entry point on
`feat/project-home-m1`; final source/check SHAs belong in its PR audit.
Manifest, dependencies, Room v18, provider/storage contracts, APK/signing/install
and Android CI are unchanged. Existence in source and runtime validation are
different; see [QA baseline](../qa/BASELINE.md).

## Product and implemented paths

RenkinAI is a personal Renkin fork that turns app/pack/uploaded icons into a real
installable icon-pack APK on the phone, with optional paid external AI generation.
Ordinary icon creation/build can work offline; AI requires the chosen provider.

| Implemented area | Source evidence and boundary |
| --- | --- |
| Existing app list/editor | [MainActivity](../../app/src/main/kotlin/dev/renkinProject/renkin/MainActivity.kt), MainViewModel and UI; launcher components from InstalledAppCatalog, including user profiles; keys are package/activity, not labels |
| Projects entry point | [ProjectHome](../../app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectHome.kt) reads the real profile flow; ID-keyed cards show name, description, saved unbuilt flag and active-session unsaved edits; default/empty/loading/unavailable/busy states are explicit |
| Create / complete / classic entry | Create confirms the existing profile fields before allocation. Complete opens instructions with working active-editor and separate-profile actions, then the classic source card/picker. Both visibly defer automatic AI/style/coverage workflows; AI1/AI2 stay in the classic editor |
| Home/editor navigation | [RenkinAiRoot](../../app/src/main/kotlin/dev/renkinProject/renkin/ui/project/RenkinAiRoot.kt) uses the Activity's existing VM; editor Back returns Home after search/dialog handlers, Home clean Back exits, dirty Back offers return to editor or explicit exit without saving; no implicit save/switch/build/request on navigation |
| Pack sources, modifications, import | ApplicationProvider, IconGenerationService, IconGenerator, UploadedImageStore; custom/base/rendered separation and existing SVG/vector/adaptive paths |
| Named profiles | ProfileManager, DbApplication, DataPreferences; Room `renkinPack` v18, default id 1; settings snapshots switch with the profile, source/provenance locks remain |
| Export and installation | IconPackBuildService, IconPackBuilder, PackKeystore, ApkInstaller; calendar/clock/adaptive/themed outputs, appfilter/appmap/theme_resources; build snapshot persists to its owning profile |
| Backup/share/watch | data/transfer and data/watch plus WatchWorker/AutoBackupWorker; existing full-backup and profile-export semantics are retained |
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
- Missing means `createdIcon == null` and not locked. Fallbacks/custom icons make
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

## Planned, not implemented

Separated CreateProjectFlow/ExtendProjectFlow and real source coverage are Codex 02.
Projects-specific style/asset persistence precedes automatic style recognition,
trial icons, batch staging and AI edits. Hosted backend authentication, credits,
automatic paid scheduling and Play publication remain future work. This Home
adds no new AI endpoint, source coverage estimate, schema or pack builder.
Follow [ROADMAP](../ROADMAP.md) and the next task in [HANDOFF](HANDOFF.md).
