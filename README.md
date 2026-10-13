# Renkin AI – personal fork

AI icon generation, persistent missing-icon batches and per-app prompt instructions. [Hungarian setup, architecture and limitations](docs/AI_FORK_HU.md).
Based on the upstream Renkin project below; GPLv3 and upstream attribution retained.

Start development with [AGENTS.md](AGENTS.md), the [documentation index](docs/INDEX.md)
and [CONTRIBUTING.md](CONTRIBUTING.md). Source-backed implementation:
[current state](docs/agent/CURRENT_STATE.md); planned work: [roadmap](docs/ROADMAP.md);
latest transfer: [handoff](docs/agent/HANDOFF.md). AI needs internet access and a
user-entered provider key; ordinary icon editing and pack assembly remain local.
The unreleased Projects home lists real Renkin profiles, creates a separate profile
and offers separate Create/Extend target preparation with read-only, mapped-and-drawn
source coverage. Search/select targets, save project style text and references locally,
or explicitly confirm optional reviewed AI style analysis in Style Setup. The new
Style Trials source prepares up to three separately confirmed icons, preserves
original/RAW history and explicit style approval without Apply or batch. The tested
implementation is in [PR #6](https://github.com/Zsombor0918/RenkinAI/pull/6), awaiting
owner review/merge. Public backend/credits remain future work. Device/provider
acceptance is tracked separately in the current-state and QA documents.

Trial walkthrough: choose apps → **Save project target selection**; open **Project AI style**,
enter a manual description under **Advanced: edit style and references** or review optional analysis,
then **Save**. **Style trial icons** is visible at the top and explains missing
prerequisites. Choose one to three apps → **Review frozen trial inputs** → the
separate paid confirmation. Compare Original/Generated, Accept/Reject, then approve
the common style separately. Local sampling, navigation and Save send no AI request.
OpenAI trials now require one standalone target icon: original image1 supplies the
symbol, later images supply visual style only, never their logos. MIXED source motifs
stay observations. Old collages remain unchanged: mark them **Not suitable** and
separately confirm any new paid retry. The corrected Flip8 one-icon acceptance is
still required before merge; structural PNG validation cannot judge recognition.

The overview, screenshots and release badges below originate from **upstream
Renkin**, not a published RenkinAI release. Fork builds come from this repository's
source/Actions; do not install an upstream APK expecting the AI features.

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="96" alt="Renkin">
</p>

<h1 align="center">Renkin</h1>

<p align="center">Make your own Android icon pack, on your phone.</p>

<p align="center">
  <a href="https://github.com/renkindevbugs/Renkin/actions/workflows/ci.yml"><img src="https://github.com/renkindevbugs/Renkin/actions/workflows/ci.yml/badge.svg?branch=dev" alt="CI"></a>
  <a href="https://github.com/renkindevbugs/Renkin/releases/latest"><img src="https://img.shields.io/github/v/release/renkindevbugs/Renkin?label=release" alt="Latest release"></a>
  <a href="LICENSE"><img src="https://img.shields.io/github/license/renkindevbugs/Renkin" alt="License"></a>
</p>

---

Renkin builds a real, installable icon pack from your own choices — no PC, no account, offline.
Pick icons from packs you already have, redraw or recolour them, then compile and sign the pack
on-device and select it in your launcher.

Fork of [Alembicons](https://codeberg.org/kaanelloed/Alembicons) by kaanelloed (formerly
*Iconeration*), with its own direction on top.

## Download

Upstream Renkin APKs are available from [upstream GitHub Releases](https://github.com/renkindevbugs/Renkin/releases/latest).
For this fork, follow the [Hungarian build guide](docs/AI_FORK_HU.md) or inspect
completed [RenkinAI Actions](https://github.com/Zsombor0918/RenkinAI/actions) artifacts.

## Screenshots

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="24%" alt="Home — pick a source and refresh">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="24%" alt="Pack preview before building">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="24%" alt="Per-app icon picker">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="24%" alt="Modifiers — scale, colorize, icon shapes">
</p>

## Features

- Source an icon from any installed icon pack, the app's own icon, or its name as text
- Modify icons: path tracing, edge detection, colorize, background removal, vector editing
- **Outline icons** — draw a coloured contour around any icon (or recolor the outline it
  already has), with a finger-painted eraser for the areas the outline should skip
- **Icon shapes** — crop any icon into Material You shapes (circle, squircle, pebble, …) or
  put it on a coloured shape plate
- Text icons with custom text, letter case and any system font (or the bundled Arcticons Sans)
- Import your own image per app — SVG included, rendered sharp or imported as editable
  vector paths; use an app's vector or monochrome layer when it has one
- Themed (Material You) icon export
- **Profiles** — several named icon sets, each with its own built pack, side by side
- **Profile sharing** — export a profile as a file anyone can import as a new profile.
  Icons from paid packs stay locked on the other device until that pack is installed
  there — and unlock automatically if the pack leaves the store (built packs carry icon
  provenance, so this can't be laundered through Renkin's own output)
- **Backup & restore** — one file with every profile, setting, watch rule and upload,
  for moving to a new phone
- **Icon watch** — get notified when a followed pack adds an icon for one of your apps
- Calendar day icons that rotate with the date
- **Live clock icons** — a clock icon taken from a pack with dynamic-clock support keeps
  ticking on launchers that support it (Nova, Lawnchair); everywhere else it falls back
  to a static icon automatically
- Built packs speak every launcher dialect: appfilter.xml plus the GO-family
  appmap.xml / theme_resources.xml and the classic icon_pack resource array

## Build

Kotlin · Jetpack Compose (Material 3) · Hilt · Room. The pack APK is assembled and signed
entirely on-device.

```
JAVA_HOME="<jdk-17>" ./gradlew assembleRelease
```

Architecture, persistence and the gotchas: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
Offline infrastructure checks: `python3 -m unittest discover -s scripts/tests -v`.
Required checks and observed main-protection status: [agent workflow](docs/agent/WORKFLOW.md).

## License

GPLv3, same as upstream. See [LICENSE](LICENSE).
