# ADR0013 - reference-first guided style

Date: October17,2026. Status: owner-authorized Phase3A, review pending.

## Context

The owner explicitly authorizes a local visual-reference workflow after completed
PR8 source38311206. A cloud analysis is not necessary to use existing manually
reviewed style revisions with the M5 Trial pipeline. Reference usefulness and
visual fidelity cannot be inferred from structured JSON validity.

## Decision

The guided style screen opens the existing editable brief/reference form. Only a
new unsaved project receives neutral starting guidance, without invented visual
properties. Existing saved text, instruction and ordered references are hydrated
unchanged. EXTEND offers up to12 locally rendered/deduplicated candidates; the
user chooses at most five and orders them with existing controls. Nothing fills
unused slots or automatically chooses a mixed family. CREATE keeps manual PNG
import. A text-only manual style remains possible with explicit visible guidance.

Sampling, selection and Save have no provider/key dependency. Transient draft
provenance binds owner/source/setup/reference stamps and actual drawable hashes;
Save revalidates current source bytes. Existing private assets/revisions remain
authoritative. There is no database migration or second reference store.

Legacy, Quick and Standard remain explicit optional methods with separate paid
consent. Method switching is blocked while unsaved edits would be lost; saved
history is never automatically adopted. Existing DNA readers/journals and Trial
consumed markers remain. Reference-first styles reach the existing target/original
resolver and frozen Trial boundary without an analysis request. Generation prompt
architecture remains unchanged; Phase4 and paid quality experiments are separate.

## Verification and limits

ADR0012 scheduling remains: one authoritative full Linux suite/build, Python and
Android separately. Existing tests remain; extend current Room/guided fixtures
and one emulator journey for zero-POST Save, ordering, restart and original bytes.
Record actual exact-head evidence externally. No physical-device, visual-fidelity,
live-provider or installed-certificate compatibility claim follows from mocks.

## Alternatives and rollback

Keeping mandatory Quick analysis imposes an unjustified paid prerequisite. A new
store/schema/editor duplicates working code. Revert the ordinary integration
commits to restore presentation; saved manual revisions remain compatible with
existing readers, with no historical data or signing rewrite.
