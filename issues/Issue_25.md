# Issue 25: Make Default Fact Parsers User Configurable

## Context

`@RetroFact` selects `AutoDetectParser` when no parser is declared explicitly.
`GearResolverFactory` currently interprets that marker through a private,
hard-coded `autoDetectParser(...)` method. The built-in selection is based on
the declared field type and currently covers:

- `String` through `StringParser`;
- `int` and `Integer` through `IntParser`;
- `Path` and collections of `Path` through `PathParser`;
- enum types through a type-specific `EnumParser`.

The method has carried a TODO since the initial commit to turn this behavior
into a configurable factory so users can supply their own default parsers. The
GitHub issue was created immediately after `PathParser` was added to the
hard-coded defaults, which confirms that this auto-detection path is the
issue's subject.

Field-level `@RetroFact(parser = ...)` already supports explicit parser
selection. This issue concerns what happens only when a fact leaves its parser
at the `AutoDetectParser` default.

## Intent

- Extract automatic parser selection from `GearResolverFactory` into a public,
  reusable extension point.
- Preserve the existing built-in parser choices as the default behavior.
- Let a collection or application replace or extend those choices through
  model construction.
- Keep an explicitly selected field-level parser authoritative.
- Support decisions that require the fact key, raw field type, and generic
  element type rather than limiting configuration to a simple raw-type map.
- Keep parser selection deterministic and report actionable failures when no
  default parser supports a fact declaration.

## Boundary With Fact Catalog Configuration

Catalog selection is a separate concern. A catalog does not select a fact
parser; it supplies data to a `CatalogFactParser` that a field has already
selected.

The catalog API has therefore been made explicit while preparing this issue:

- `@RetroFactCatalog` replaces the misleading `@RetroFactParser` name.
- Its `parser` member accepts only `CatalogFactParser<?>` implementations.
- `FactCatalogConfiguration` replaces `FactParserConfiguration`.
- `Model.Builder.factCatalog(...)` replaces `factParser(...)`.

These names reserve parser-default vocabulary for the extension point that
Issue 25 will introduce and prevent catalog configuration from appearing to
participate in parser selection.

## Required Precedence

1. A parser explicitly declared by `@RetroFact(parser = ...)`.
2. User-configured default parser selection for a fact that retains
   `AutoDetectParser`.
3. The framework's built-in default parser selection.
4. An actionable unsupported-type failure when no selection applies.

User configuration must not alter facts that select their parser explicitly.

## Open Design Questions

- Whether the public extension point is one replacement factory or an ordered
  chain in which user selectors can defer to framework defaults.
- Which immutable public request type exposes the fact key and declared Java
  type without leaking the internal `FactDescriptor`.
- Whether portable annotation configuration is useful in addition to runtime
  `Model.Builder` configuration.
- Whether selectors return parser instances, parser classes, or parser
  factories, and how that choice interacts with framework-owned construction.
- Whether configured catalog-backed defaults should use the same
  `CatalogLoader` construction path as explicitly selected catalog parsers.

## Progress

- [x] Recovered and documented the issue's original intent.
- [x] Separated fact-catalog configuration vocabulary from parser selection.
- [x] Restricted catalog overrides to `CatalogFactParser<?>` implementations.
- [ ] Agree on the public default-parser selection contract.
- [ ] Implement model-level configuration and precedence.
- [ ] Add focused tests for replacement, fallback, generic types, and explicit
      parser precedence.
- [ ] Update public documentation with configuration examples.

## Verification

After the catalog-boundary cleanup:

- `mvn -pl retro-crawler-core test`: 139 tests passed.
- `mvn test`: all seven reactor modules passed.
