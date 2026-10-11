# Current implementation

Source baseline: `eb4daa5de82fab54e115363c073eb8981f2203d6`, inspected October 11,
2026. Codex 00 changes documentation and development infrastructure only; no Kotlin,
manifest, dependency, Room schema, signing or Android CI behavior is changed.
Existence in source and runtime validation are different; see [QA baseline](../qa/BASELINE.md).

## Product and implemented paths

RenkinAI is a personal Renkin fork that turns app/pack/uploaded icons into a real
installable icon-pack APK on the phone, with optional paid external AI generation.
Ordinary icon creation/build can work offline; AI requires the chosen provider.

| Implemented area | Source evidence and boundary |
| --- | --- |
| Existing app list/editor | [MainActivity](../../app/src/main/kotlin/dev/renkinProject/renkin/MainActivity.kt), MainViewModel and UI; launcher components from InstalledAppCatalog, including user profiles; keys are package/activity, not labels |
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
- Source supports backup/migrations; this infrastructure PR does not test an
  actual device upgrade or guarantee a reproducible CI debug signing identity.

## Planned, not implemented

ProjectHome/project onboarding, redesigned AI flows, hosted backend authentication,
credits/billing, server-side spend enforcement, automatic/background AI scheduling
and Play publication are future work. Profiles already exist; a project home screen
does not. Follow [ROADMAP](../ROADMAP.md) and the next task in [HANDOFF](HANDOFF.md).
