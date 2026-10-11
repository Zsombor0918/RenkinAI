# ADR 0002 — profiles as projects and one editor session

Date: October 11, 2026. Status: proposed for owner acceptance with Codex 01.

## Context

The accepted product contract makes an existing Renkin profile the project identity.
MainActivity already owns a MainViewModel created before Composition; its singleton
provider owns the live icons. Profile switches asynchronously swap preferences/icons
and reset the VM baseline. Calling a Unit-returning method is not load completion.

## Decision

Use a small saved Home/Editor shell around the existing MainColumn, preserving one
Activity/VM/provider session. Display real profiles keyed by ID, not labels. Keep
editor UI primitives per ID and keep drawable/domain state outside saved UI state.
Home requests delegate through a transient controller owned by MainViewModel;
successful provider load plus baseline reset explicitly precede Editor navigation.

Preserve the existing session mutex, check the origin and dirty edits inside it,
save only on approval, and permit Cancel without allocation. Block repeated requests
synchronously; never replay creation during recomposition or process restoration.
An interrupted committed profile stays discoverable in the existing Room list.
This is recovery through existing identity, not a claimed cross-store transaction.

Create and Complete are equally visible classic-editor entry paths for M1. Complete
explains real source selection and provides working continuation/separate-profile
buttons. Exact source coverage, separated flows and automated AI remain later work.

## Alternatives

- A new project database/session or second editor: duplicates profile identity and
  risks losing existing icon/package/signing continuity; rejected.
- A navigation framework for two routes: adds dependencies without resolving session
  completion; deferred until the route count/lifecycle needs justify it.
- Navigate after a fixed delay or immediately after `switchProfile`: races baseline
  loading, errors and activity recreation; rejected.
- Automatically repeat an interrupted Create: can duplicate a committed Room row;
  rejected. Show interruption and recover by selecting its real ID instead.
- Keep a hidden MainColumn composed on Home: leaves Back handlers/tutorials/modal
  effects active on an invisible screen; rejected. Explicit editor attention opens
  their actual owner route.

## Consequences

No schema/migration, profile-store, key, builder, installer or AI endpoint change.
The old MainColumn call remains a one-line rollback route with legacy Back behavior.
Saved-state recovery does not persist unsaved icons; explicit save/build remains
necessary. Robolectric contracts need device confirmation for notifications,
external Activities, accessibility and Samsung/fold behavior.

State diagram, integration details and tests: [ARCHITECTURE](../ARCHITECTURE.md),
[change note](../changes/2026-10-11-project-home.md), [QA](../qa/BASELINE.md).
