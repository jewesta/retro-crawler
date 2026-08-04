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
- Its `parser` member accepts only `CatalogFactParser<?, ?>` implementations.
- `FactCatalogConfiguration` replaces `FactParserConfiguration`.
- `Model.Builder.factCatalog(...)` replaces `factParser(...)`.

These names reserve parser-default vocabulary for the extension point that
Issue 25 will introduce and prevent catalog configuration from appearing to
participate in parser selection.

## Typed Parser Contract

The parser SPI groundwork is independent of selecting default parsers and has
been completed first:

- `FactParser<T>` identifies the type of one interpreted fact value and returns
  `RatedFact<T>`.
- `RatedFact<T>` represents zero or one parsed value. A successful result
  contains one `T`; a `NONE` result contains none.
- One parser invocation interprets exactly one raw clue value. Only
  `FactFinder` aggregates the independently parsed raw values into the final
  `Fact` value set.
- The `exact`, `strong`, and `weak` factories therefore accept one `T`. There
  are no collection-valued parser-result factories. In particular, a
  `FactParser<Color>` cannot accidentally return a `Set<Color>` and ask the
  framework to flatten it.
- `CatalogFactParser<K, T>` carries both its catalog-key type and parsed value
  type.
- Framework locations that intentionally aggregate unrelated parsers use
  `FactParser<?>` and `RatedFact<?>`; concrete parsers retain their exact result
  type.

This phase does not change `AutoDetectParser` selection or introduce any
default-parser configuration. It only gives that future extension point a
typed parser contract to expose.

## Decided Selection and Construction Model

Parser selection and parser construction are separate:

- `@RetroFact(parser = ...)` explicitly selects a parser class.
- For a fact that retains `AutoDetectParser`, `@RetroFactDefaultParser` selects
  the parser class for each supported value type. Its members are typed, for
  example `Class<? extends FactParser<String>> string()`, and default to the
  current built-in implementations.
- Fixed-type defaults do not need marker roles such as `DefaultStringParser`.
  Enum defaults use the specialized `EnumFactParser<T>` role because one
  annotation member must accept parsers for different concrete enum types.
  Its deliberately raw `Class<? extends EnumFactParser>` boundary is narrowed
  using the enum type declared by each fact field.
- The framework passes that declared enum type to a public `Class` constructor
  when constructing an annotation-selected enum parser. It trusts the parser's
  declared contract to return that enum type.
- After selecting a concrete parser class, explicit and automatically selected
  parsers otherwise use the same construction path.
- `Model.Builder` can register a per-key construction factory for any selected
  parser class. The public shape is:

  ```java
  <T> Builder parserFactory(
      Class<? extends FactParser<T>> parserType,
      Function<String, ? extends FactParser<T>> factory)
  ```

- The factory receives the effective RetroCrawler fact key. It is queried once
  per effective key, and the resulting parser is retained by that key's
  `FactFinder`.
- A factory may return a different implementation of `FactParser<T>` than the
  selected class. This allows runtime construction and replacement without
  weakening the parsed-value type.
- If no factory is registered for the selected class, framework construction
  falls back to the class's supported reflective construction path.

For example, both a default-selected and an explicitly selected
`StringParser.class` are constructed through the same registration:

```java
.parserFactory(StringParser.class, MyStringParser::new)
```

`parserFactory(...)` is intentionally about construction. Parser selection
remains annotation-driven.

## Required Precedence

1. A parser explicitly declared by `@RetroFact(parser = ...)`.
2. The configured default parser class for a fact that retains
   `AutoDetectParser`.
3. The framework's built-in default parser class.
4. For the selected class, a builder-registered construction factory.
5. Framework reflective construction when no factory is registered.
6. An actionable failure when selection or construction is unsupported.

An explicit field-level parser remains authoritative as the selected parser
class. Builder configuration may customize how that selected class is
constructed, just as it may for a default-selected class.

## Open Design Questions

- Whether configured catalog-backed defaults should use the same
  `CatalogLoader` construction path as explicitly selected catalog parsers.

## Progress

- [x] Recovered and documented the issue's original intent.
- [x] Separated fact-catalog configuration vocabulary from parser selection.
- [x] Restricted catalog overrides to `CatalogFactParser<?, ?>`
      implementations.
- [x] Made `FactParser<T>` and `RatedFact<T>` type-safe for one parsed value per
      raw observation.
- [x] Restored `FactFinder` as the sole aggregation boundary and removed
      collection-valued parser results.
- [x] Replaced the collection's compound color marker with separate comma-
      delimited clue values.
- [x] Migrated built-in, shared-model, demo, and collection parsers to typed
      results.
- [x] Agreed on annotation-based default-class selection and general per-key
      builder factories for selected parser classes.
- [x] Implemented `@RetroFactDefaultParser` for string, integer, and path
      defaults, including collections of paths.
- [x] Implemented the enum default through `EnumFactParser<T>`, with the
      concrete field enum supplied during reflective parser construction.
- [x] Implemented typed `Model.Builder.parserFactory(...)` construction for
      both default-selected and explicitly selected parser classes.
- [x] Added focused tests for annotation fallback, per-key factories, generic
      replacement, explicit selection, duplicate registration, and null
      results.
- [ ] Update public documentation with configuration examples.

## Verification

After the typed parser-contract refactor:

- `run/prettify.sh --apply ...`: changed Java sources processed.
- `mvn -pl retro-crawler-core,retro-crawler-model,retro-crawler-mycollection -am test`:
  250 tests passed.
- `mvn clean install`: all seven reactor modules and 253 tests passed.

After implementing configurable fixed defaults and per-key parser factories:

- `run/prettify.sh --apply ...`: all changed Java sources processed.
- `mvn -pl retro-crawler-core test`: 146 tests passed.
- `mvn clean install`: all seven reactor modules and 259 tests passed.

After enabling the configurable enum default:

- `run/prettify.sh --apply ...`: all changed Java sources processed.
- `mvn -pl retro-crawler-core test`: 148 tests passed.
- `mvn clean install`: all seven reactor modules and 261 tests passed.
