# Issue 28: Add Configurable Temporal Default Fact Parsers

## Context

`@RetroFactDefaultParser` currently supplies configurable defaults for strings,
integers, paths, and enums. Archive clues commonly contain temporal values too,
including exact instants and calendar dates such as `2024-01-30`, `30.01.2024`,
or `30 January 2024`.

Parsing those values is influenced by more than a parser class. Locale, region,
time zone, and clock are general interpretation settings that can also matter to
future parsers for money, numbers, localized names, and relative values. They
therefore belong to general parser configuration rather than to a date-specific
API.

The existing `FactParseContext` already provides a context object at parser
invocation time. Its original API weakened that seam by making the context-free
`FactParser.parse(String)` method primary and providing a default contextual
method which ignored the context. Runtime resolution nevertheless always has an
archive location. The context-free resolver and finder conveniences were unused
outside tests and did not represent a runtime requirement.

## Intent

- Add `Instant` and `LocalDate` to the built-in configurable default parsers.
- Parse common date representations with the JDK date/time API and no new
  dependency.
- Give every fact parser access to immutable interpretation configuration
  through its invocation context.
- Use the host's format locale, time zone, and clock as convenient defaults,
  captured at a deterministic configuration boundary.
- Allow annotations and `Model.Builder` configuration to override those
  defaults deterministically.
- Preserve explicitly implemented `FactParser<T>` behavior and keep the
  parser API small and client-facing.

## Design Decisions

- `LocalDate` represents a complete calendar date without inventing a time or
  zone. Existing `DateMarking` remains the appropriate shared value for partial
  evidence such as only a year, month, or ISO week.
- `Instant` represents an exact point on the time line. Offset-bearing ISO input
  and epoch milliseconds are suitable defaults; a date-only value must not be
  silently assigned a time zone.
- Interpretation configuration is general parser configuration, not date-parser
  configuration.
- The parser invocation context is the existing extension seam for both general
  configuration and the current archive node. The exact public contract remains
  design work; the current proposal is a compact `ParseContext` exposing
  `config()` and `currentNode()`.
- Automatic regional defaults should follow the host's format locale and system
  time zone so the naive laptop case behaves as expected. Explicit configuration
  must remain possible through annotations and the model builder.
- Locale-sensitive date parsing is strict. If configured candidate formats
  produce different valid dates for the same input, parsing must reject the
  ambiguous value rather than selecting one accidentally.
- Runtime fact resolution is always located in an archive. Detached resolution
  is not a supported runtime use case.

## Progress

- [x] Agreed on separate `LocalDate`, `Instant`, and existing `DateMarking`
      semantics.
- [x] Chosen the JDK date/time formatter facilities rather than a third-party
      parsing dependency.
- [x] Chosen general interpretation configuration with automatic regional
      defaults.
- [x] Made `FactParser.parse(String, FactParseContext)` the sole parser method.
- [x] Removed context-free `FactFinder` and `GearResolver` overloads and the
      `FactParseContext.detached()` convenience.
- [x] Updated parser implementations, delegates, and tests to pass the context
      explicitly.
- [ ] Finalize the public `ParseContext`, `Configuration`, and current-node
      contracts.
- [ ] Implement immutable interpretation configuration.
- [ ] Add annotation and builder configuration with documented precedence.
- [ ] Add default `LocalDate` and `Instant` parser selection.
- [ ] Add focused construction, locale, ambiguity, and temporal parsing tests.
- [ ] Update public documentation and verification results.

## Open Questions

- Whether localized parsing should initially cover only numeric and JDK
  localized date styles or also add explicit textual formatter patterns beyond
  what the JDK localized styles provide.
- Whether epoch milliseconds should be accepted by the default `Instant` parser
  or require an explicitly selected parser because an unadorned integer has no
  intrinsic unit.
- What the parser-facing `Node` type must expose, and whether it should be the
  existing `ArchiveNode` or a narrower public view.
- Whether `FactParseContext` should evolve into `ParseContext` directly or be
  replaced once the new contract is settled.

## Verification

- Canonical `prettify` formatter applied to all changed Java sources.
- `mvn test`: reactor successful, 260 tests run with no failures or errors.
