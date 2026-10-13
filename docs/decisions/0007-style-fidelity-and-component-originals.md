# ADR 0007 — bounded local style evidence and verified component inputs

Date: October13,2026. Status: proposed for owner review in existing PR#6.
Baseline:422ef578, accepted main af7bdb10. Owner enhancement ticket6149883090.

## Context and verified before behavior

InstalledAppCatalog already reads unbadged LauncherActivityInfo.getIcon(density), records exact component keys and rejects ambiguous/non-current users through coverage. TrialOriginals already reads PackageManager.getActivityInfo/getActivityIcon independently of EXTEND. MainViewModel supplies this owner.original method; OwnedStyleTrials freezes bytes/hash/origin and currentness. Classic editor hero/app icons are catalog originals; its editable base/custom/source preview intentionally follows profile selection. AI1/AI2 retain their existing caller-selected source bitmap and request bytes. Neither editor semantics nor legacy source behavior is changed here.

| Meaning | Actual source and use |
| --- | --- |
| APP_COMPONENT_ORIGINAL | M5 exact ActivityInfo declared full icon resource; current-user, density and source revision checked; frozen input image1 and original history |
| SOURCE_PACK_STYLE | EXTEND parser coverage/resources, local analysis and explicitly saved ordered references; never target identity |
| PROFILE_CUSTOM | Existing created/base/rendered/manual icons and locks; preserved, protected from recommended trials |
| CURRENT_LAUNCHER_DISPLAY | Home/Theme Park presentation; not read by M5; a public icon API may return substituted pixels and is compared as evidence |

The red resource test deliberately injects themed activity API A while the owning alias resource C and source artwork B differ. This proves a simulated limitation, not a Samsung-specific vendor diagnosis. Red sampling tests prove hue distortion and minority over-selection in current source.

## Decision

Use deterministic alpha-aware 64px statistics of bounded256px PNGs. Accumulate actual RGB within4-bit buckets, then merge in Oklab using a0.035 distance heuristic with at most24 seeds/six retained swatches. No bin-midpoint colors. Discard alpha<128 from palette roles; weight brightness/saturation/contrast by visible alpha. Bound geometry families by occupied area, minimum padding, edge and empty-center heuristics independently of hue/theme brightness. Allocate proportional medoids plus minority checks; no uncalibrated confidence. Report min/median/max optical distributions and unknown precise stroke/corner/shadow/gradient traits.

Actual EXTEND controller evaluates up to300 uniformly distributed source mappings, adaptive to source size; legacy sampler default120 stays for compatibility. Keep an8MiB PNG sketch, <=300 feature descriptors,12 selected256px thumbnails/384KiB each/4MiB aggregate, existing5MiB HTTP and64KiB journal bounds. Evicted selected PNGs rerender once and must match their hash/source; no paid second pass. Dynamic/unstable sources, cancellation, theme/package/index staleness and all old exclusions remain. Benchmarks use50/150/500 synthetic mappings and compare120/240/300; real-device latency/RSS are not inferred.

Keep StyleProfileV1 unchanged. Color-policy semantics fit its editable short description and uncertainty/identity notes: FIXED_PALETTE, ADAPT_BRAND_COLORS, MONOCHROME/DUOTONE, MIXED/UNKNOWN. A few brand-logo colors cannot become mandatory pack-wide swatches. Display local observed roles/prevalence separately from model inference, with explicit unknowns. Add optional bounded local evidence schema1 to sample reports and persist the analysis algorithm only for new journals; absent algorithm parses as legacyv1, preserving historical fingerprints and consumed IDs. Accepted V1 style/backups stay compatible; no Room migration.

Keep the same Responses model, low detail, strict V1 schema, store=false, one-shot POST and independent consent. The displayed actual sample count stays authoritative. Local measurements add bounded text input; consent describes it and exact cost remains unknown. Safe low/high golden comparison tests JSON shape only; high is rejected by current production validation. OpenAI non-null edit prompts retain one authoritative single-target contract and add native-color priority unless user-reviewed fixed/mono/duotone recoloring is intentional. Null-override legacy and Stability composeSaved remain byte-equivalent.

For M5 resolve the exact enabled current-user component resource via its owning application Resources.getDrawableForDensity, including labelled application-icon fallback only for a real existing component with no own resource. Use full adaptive drawable, not monochrome or badged/screenshot/source/custom artwork. Freeze optional original-evidence schema1 (source/type/user/density/resource/API agreement) in new TrialInput JSON; absent evidence preserves old plan encoding. Preserve old origin when raw resource and activity API bytes agree; use component-resource-v2 only for actual differing input. Historical approvals are not invalidated just by new optional fields. Original hash/origin/evidence is freshly compared before spending.

Package Resources can themselves have OEM overlays. All new resource inputs therefore display UNVERIFIED_ORIGINAL and need a plan-specific conscious original comparison before separate paid consent, checked in both UI and owned controller. This checkbox authorizes only the displayed original choice, never a POST by itself. No recoverable exact resource means no paid plan; no invented art or source-pack substitution. No mandatory global source picker, permission, internet fetching or work-profile bypass.

## Alternatives and consequences

A larger remote image count/high-detail/model switch has unproven quality/cost tradeoffs and is rejected. A StyleProfileV2/Room migration is unnecessary because existing reviewed text conveys the policy. Maximum diversity biases dominant packs. Scraping app-store icons changes permissions/privacy and is unnecessary. Claiming API agreement proves pristine artwork is false; visual owner verification remains necessary. The added checkbox is intentionally local to frozen originals, with no new persistent preference.

Reverting optional journal parsers without compatibility would hide new records; use a forward repair preserving legacy/new parsers, consumed UUIDs, source/owner guards and data. Existing databasev21, full backupformat6/payload3, signing identities, editor/profiles/import/watch/build/install and M6 read-only acceptance stay intact. Owner device acceptance and merge are separate from automated green checks.

## Primary references read

[LauncherActivityInfo unbadged icon](https://developer.android.com/reference/android/content/pm/LauncherActivityInfo),
[PackageManager component icon](https://developer.android.com/reference/android/content/pm/PackageManager),
[adaptive full versus monochrome layers](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive),
[OpenAI vision detail modes](https://developers.openai.com/api/docs/guides/images-vision),
[image identity/style prompting](https://developers.openai.com/api/docs/guides/image-prompting),
[W3C CSS Color4 Oklab](https://www.w3.org/TR/css-color/).
These describe public contracts, not vendor overlay immunity or a guaranteed model quality gain.
