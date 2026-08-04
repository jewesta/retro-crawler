# Issue 34: One Crawler, Multiple Archives

## Intent

Let one `RetroCrawler` apply a shared `Model` to several independently
identified archives. Each archive owns its descriptor, root, source provider,
repository identity, and crawl lifecycle while sharing clue interpretation,
fact parsing, gear matching, and model configuration.

The target composition is:

```text
RetroCrawler
├── Model: retro hardware
├── Archive: my_collection
│   └── filesystem root
├── Archive: museum_collection
│   └── SSH root
└── Archive: incoming_material
    └── ZIP root
```

## The Multi-Root Question

The first implementation kept `@RetroCollection.locations` and let one
`ArchiveDescriptor` hold several roots ("buckets"). That left RetroCrawler with
two nested levels of the same idea: archive-over-roots and crawler-over-archives.
They were reviewed against each other and the lower level was removed.

### Why locations lost

1. **Archives strictly subsume roots.** An `ArchiveSource` binds per archive,
   never per root. A collection split across a local disk and a ZIP already
   required several archives; roots only ever handled the homogeneous case.
   Locations were a weaker archive.
2. **Everything roots provided is aggregation policy, not rootedness.** Shared
   repository entry, shared Retro ID namespace, one result tree — all of these
   are properties of *how results are combined*, and belong at the level that
   has identity. They are now crawler operations (`crawlAll`).
3. **A planned query API is cross-archive by definition** because the crawler
   owns the model. Every result needs addressable provenance. `ArchiveId` is a
   stable identity with a display name; a bucket's `basePath` is deployment
   state that `bind` rewrites, and its index is meaningless to a reader. Making
   roots addressable would have meant giving them ids and names — that is an
   archive.

### What Bucket contributed, and where it went

`Bucket` did two jobs; only one was multiplicity.

- **Anchoring a relative tree to a rebindable base path.** `ArchiveNode` is pure
  relative structure; the anchor makes portable relative source paths, a
  model-independent persisted clue archive, and cache relocation possible. This
  survives verbatim as `Archive.basePath`.
- **A rehearsal for archives.** Because every consumer already handled *n*
  independently anchored trees emitted as separate groups, the multi-archive
  change was mechanical. Promoting buckets to archives cashes that investment.

What died is the `List<>` around it: `bucketIndex`, `findBucket`, the
stored-count-vs-configured-count check, the "subtree belongs to more than one
configured root" ambiguity, and `beginBucket`/`endBucket` in their per-root
meaning.

### What it costs

The homogeneous split — same provider, several volumes — was cheaper as roots:
one cache entry, one crawl, one namespace. It now costs one archive per volume
plus crawler-level aggregation. `crawlAll` is what pays for it.

## Decisions

1. `ArchiveDescriptor` is `(ArchiveId id, String name, Path root)`. One archive,
   one root.
2. `Model` carries no archive at all. `@RetroCollection` declares collection
   identity and interpretation; `locations` is gone. Where a collection lives is
   deployment configuration, resolving the pressure recorded in Issue 24.
3. `Model` exposes `collectionId()` and `collectionName()` so the annotation's
   identity still names the whole collection across its archives.
4. Every crawler registers at least one archive explicitly. The builder has one
   configuration mode; `archiveSource(...)` and its mutual-exclusion guards are
   gone.
5. Archive-unqualified `crawl...`, `inspect`, and `archiveDescriptor` shortcuts
   are removed. They traded a compile error for a runtime error on exactly the
   composition the API is designed for.
6. `crawl(archiveId, ...)` resolves one archive; `crawlAll(...)` resolves every
   registered archive in one pass.
7. Retro ID uniqueness is validated across everything a single call resolves:
   crawler-wide for `crawlAll`, per-archive for `crawl(archiveId, ...)`.
8. A `SUBTREES` reindex scope passed to `crawlAll` is routed to the archive whose
   root contains each requested path. A path below no registered root is
   rejected. Archives without a requested subtree reuse their stored archive.
9. `GearTreeFactory` groups per archive: `beginArchive`/`endArchive` receive the
   `ArchiveDescriptor`, which carries identity, display name, and root.
10. `Stash` holds `ArchiveGear` per archive, retaining provenance for every
    result. `StashStats.bucketCount` became `archiveCount`.
11. The persisted clue archive is `{version, id, basePath, root}`. Cache
    relocation is unconditional rebinding of one base path.

## Incidental Fixes

Re-indexing an archive's own root as a subtree previously failed: relativizing a
path against itself yields a one-element empty path, so both
`ArchiveManager.relativeFolders` and `ArchiveDigger.target` looked for a child
folder named `""`. Both now treat root-equals-path as the root target. This
surfaced through subtree routing in `crawlAll`.

## Consequences for Existing Deployments

Stored clue archives are not compatible: the persisted shape changed and archive
IDs are now per archive rather than per collection. Every deployment re-indexes
once. `MyCollectionSmokeCrawl` now registers one archive per line of its roots
file, deriving each ID from the root's own folder name, and crawls with
`crawlAllGear` so duplicate Retro IDs are still detected across the whole
collection.

## Progress

- [x] Add multi-archive crawler composition and archive-selected operations.
- [x] Collapse archives to exactly one root; remove `ArchiveRoots` and `Bucket`.
- [x] Remove archive locations from `@RetroCollection` and from `Model`.
- [x] Add crawler-wide `crawlAll` with cross-archive Retro ID validation and
      subtree routing.
- [x] Regroup the gear tree factory, `Stash`, and stats per archive.
- [x] Demonstrate one shared model with filesystem and ZIP archives.
- [x] Document the public composition model.
- [x] Run focused and reactor verification.

## Verification

- Canonical `prettify` assertion passed for all changed Java sources.
- `mvn test` passed for the full reactor: 307 tests, 0 failures, 0 errors.
- `mvn clean install` passed for the full seven-module reactor.
