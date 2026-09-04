# Issue 39: Improve MCP

## Intent

Improve RetroCrawler's MCP interface based on the implementation and field-test
experience from Issue 37.

## Gap Analysis

The first improvement restores core's folder-level re-crawl capability at the
MCP boundary. `ReindexScope` already accepts one or several folder ARIs, while
the original `start_crawl` tool could select only complete archives.

## Decisions

1. Extend `start_crawl` with an optional `subtreeAris` selection rather than
   introducing a second crawl tool.
2. Keep `archiveIds` and `subtreeAris` mutually exclusive. Omitting both, or
   supplying only empty lists, retains the complete-crawl default.
3. Accept canonical archive resource identifiers, never provider filesystem
   paths. Validate their syntax, collection identity, archive identity,
   uniqueness, length, and count before starting an asynchronous operation.
4. Let core validate whether a selected ARI identifies a folder present in the
   stored clue archive. That validation belongs to the archive and remains an
   operation failure rather than causing MCP to inspect physical paths.
5. Retain the original one-argument Java overload for source compatibility;
   the annotated MCP method publishes the new two-property input schema.
6. Add `browse_archive` over the published Stash's indexed folder observations.
   Its required `ari` identifies the folder being browsed; results contain a
   bounded page of direct children rather than recursively serializing the
   complete archive.
7. Browse every indexed folder, including folders that do not resolve into
   Gear. Overlay each folder with its direct-child count, total Gear occurrences
   at or below it, Gear kinds at the exact folder, and crawl timestamps.
8. Add each archive's canonical `rootAri` to `list_archives`, allowing clients
   to begin browsing without constructing an identifier or seeing a provider
   path.
9. Remove the collection-specific `gearKind` fact and `[HDD]` marker exposed by
   the new filter catalog. They were speculative test scaffolding rather than
   established archive vocabulary: the real collection contains no such tag,
   and no other Gear type uses an explicit type designator. Remove the
   unreachable collection `HardDiskDrive` specialization with them while
   retaining the objective shared hard-drive form-factor vocabulary.
10. Treat `AnyGearMatcher` as the unique fallback role within a model. Reject a
    model assigning it to more than one Gear type, without generally requiring
    matcher implementations to be unique: ordinary matching strategies remain
    reusable and equal best matches remain a separate runtime concern.
11. Give every Gear type stable model metadata through optional `key` and `name`
    attributes on `@RetroGear`. Derive omitted values from the simple Java class
    name (`GraphicsCard` becomes `graphics-card` and `graphics card`), require
    keys to be globally unique, and retain the metadata on resolved Stash nodes.
    Do not manufacture a clue or Fact: the type is model metadata, while the ARI
    identifies one occurrence of Gear.
12. Keep a Fact's semantic key as its sole stable identity. Add an optional
    human name to `@RetroFact`, derived from the Java field name when omitted.
    One explicit name establishes the shared name for a key; conflicting
    explicit names fail model construction. If no declaration is explicit,
    independently derived names must agree rather than silently choosing one.
13. Centralize Java-name conversion in the static `ModelNames` utility. Use
    model-provided names in the application filter bar and MCP results instead
    of having each consumer improvise labels.
14. Expose protocol terminology consistently: MCP filters have a `key` and a
    `name`, filter criteria select `filterKey`, Gear search hits expose `type`,
    and archive browsing reports `gearTypes`.

## Status

- Created `issues/Issue_39` from the updated `main` branch.
- Implemented canonical folder-ARI selection for `start_crawl`.
- Added focused mapping, validation, status-projection, and MCP parameter-name
  tests.
- Implemented bounded, paginated direct-child browsing through
  `browse_archive` and exposed archive root ARIs.
- Removed the unused `gearKind` filter source, parser, model field, and
  `HardDiskDrive` specialization after the MCP exposed the one-value filter and
  confirmed that it matches no Gear.
- Enforced the documented single-`AnyGearMatcher` invariant during model
  construction while retaining reusable ordinary matchers.
- Added stable, named Gear-type metadata and named Fact filters with validated
  defaults and overrides, then propagated both through Stash, the app, and MCP.
- Replaced MCP filter-ID and Gear-kind vocabulary with model keys, names, and
  Gear types.

## Next Improvements

- Add detailed Gear retrieval by ARI.
- Expose Stash statistics and crawl freshness.
- Improve typed filters, availability, pagination, and crawl diagnostics.

## Verification

- The canonical formatter passes for all changed Java sources.
- The focused core behavior passes with 295 tests; core and MCP together pass
  with 321 tests in the complete reactor.
- The complete nine-module `mvn test` reactor passes.
- The complete nine-module `mvn clean install` packaged reactor passes.
- The clean `retro-crawler-mycollection` Java 21 build passes with 52 tests
  after removing the speculative type marker.
