# Issue 12 — Resolve Gear matcher conflicts deterministically

## Intent

Gear selection must never depend on model discovery or map iteration order.
Equal best matcher confidence is resolved through the Gear type hierarchy when
one candidate is strictly more specific than every other candidate. A genuine
tie produces the model's optional fallback Gear; without a fallback, no Gear
can be produced.

## Domain model

- `@RetroGear` declares a Gear type whose matcher evaluates evidence.
- `@RetroAnyGear` declares the model's single optional fallback Gear. It has no
  matcher and therefore does not participate in matching.
- The fallback represents known archive existence with unresolved Gear
  identity. A collection may name and model that Gear however it chooses.
- `AnyGearMatcher` is removed rather than retained as a compatibility shim.

## Selection rules

1. Evaluate every actual Gear matcher and retain all results in the trace.
2. Consider only candidates at the highest non-`NONE` confidence.
3. If exactly one candidate remains, select it.
4. If one tied candidate is a subtype of every other tied candidate, select
   that most-specific type.
5. Otherwise record an ambiguous Gear match and select `@RetroAnyGear` when
   present.
6. When nothing matches, select `@RetroAnyGear` when present.
7. If selection needs a fallback and none is declared, produce no Gear.

## Trace

A fallback does not pretend to have matched. The resolution trace separates
matcher results from the selected Gear and records whether selection was a
direct match, hierarchy resolution, no-match fallback, or ambiguous-match
fallback. Genuine tied candidates remain visible as a matching issue.

## Progress

- Created the issue worktree and branch from current `main`.
- Added `@RetroAnyGear` to model discovery and Gear metadata, including
  uniqueness validation and the same field injection support as matched Gear.
- Removed `AnyGearMatcher` and migrated the demo, collection, and test models.
- Made matcher evaluation and tie reporting deterministic.
- Added explicit trace selection reasons without manufacturing a fallback
  match.
- Kept optional fallback state internal and exposed absence through `Optional`
  only at method return boundaries.
- Documented the new annotation in the public README.

## Verification

- Added focused tests for hierarchy-resolved ties, genuine ties, confidence
  precedence, weak real matches, no-match fallback, absent fallback, annotation
  validation, and model discovery.
- `run/prettify.sh --apply` completed successfully for all changed Java files.
- `mvn clean install` passed for all nine reactor modules: 474 tests, no
  failures or errors.
