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

## Next Improvements

- Add detailed Gear retrieval by ARI.
- Expose Stash statistics and crawl freshness.
- Improve typed filters, availability, pagination, and crawl diagnostics.

## Verification

- The canonical formatter passes for all changed Java sources.
- The focused `retro-crawler-core` and `retro-crawler-mcp` reactor passes with
  309 tests.
- The complete nine-module `mvn test` reactor passes.
- The clean `retro-crawler-mycollection` Java 21 build passes with 52 tests
  after removing the speculative type marker.
