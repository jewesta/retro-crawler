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

## Status

- Created `issues/Issue_39` from the updated `main` branch.
- Implemented canonical folder-ARI selection for `start_crawl`.
- Added focused mapping, validation, status-projection, and MCP parameter-name
  tests.

## Next Improvements

- Add bounded archive and Stash browsing.
- Add detailed Gear retrieval by ARI.
- Expose Stash statistics and crawl freshness.
- Improve typed filters, availability, pagination, and crawl diagnostics.
