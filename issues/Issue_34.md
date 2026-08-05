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
   identity names the whole collection across its archives. The collection id
   is also the namespace of every Archive Resource Identifier (`ARI`).
4. Every crawler registers at least one archive explicitly. The builder has one
   configuration mode; `archiveSource(...)` and its mutual-exclusion guards are
   gone.
5. Archive-unqualified `crawl...` and `archiveDescriptor` shortcuts are
   removed. Inspection accepts a collection- and archive-qualified ARI rather
   than a physical `Path`.
6. `crawl(archiveId, ...)` resolves one archive; `crawlAll(...)` resolves every
   registered archive in one pass.
7. Retro ID uniqueness is validated across everything a single call resolves:
   crawler-wide for `crawlAll`, per-archive for `crawl(archiveId, ...)`.
8. A `SUBTREES` reindex scope contains ARIs and is routed by their archive ids.
   The crawler rejects foreign collection ids, unknown archives, and an ARI for
   an archive outside a selected single-archive crawl. Physical root overlap is
   irrelevant. Archives without a requested subtree reuse their stored archive.
9. `GearTreeFactory` groups per archive: `beginArchive`/`endArchive` receive the
   `ArchiveDescriptor`, which carries identity, display name, and root. Its
   source-aware `addNode` receives the gear's ARI.
10. `Stash` holds `ArchiveGear` per archive and every `GearNode` retains its
    source ARI. The archive and gear trees remain deliberately different: the
    archive tree preserves accepted folders while the gear tree compresses
    non-gear nodes. `StashStats.bucketCount` became `archiveCount`.
11. The persisted clue archive is `{version, id, basePath, root}`. Cache
    relocation is unconditional rebinding of one base path. This incompatible
    shape is cache format 3.
12. `JsonFileRepository` inspects the raw top-level numeric version before
    deserializing a version-dependent `Archive`. Only a supported version is
    passed to its decoder; missing, malformed, older, and future versions fail
    at the repository boundary. `Archive` pins `version` as its first serialized
    property with `@JsonPropertyOrder`, so the preflight stops on the first
    token instead of scanning the clue tree. The pin is deliberate rather than
    incidental: Jackson would otherwise derive the order from the creator
    signature, which is not a format contract.

## Archive Resource Identifiers

An ARI is a stable URI-backed logical address:

```text
ari:/<collection-id>/<archive-id>/<archive-relative-resource-path>
```

It contains no physical root or provider scheme. A filesystem, ZIP, or future
SSH archive can therefore expose the same logical resource after a deployment
change. `RetroCrawler.identify(archiveId, sourcePath)` bridges existing
path-valued application data into that logical address.

Resolution belongs to a crawler. An ARI from another collection is rejected. A
separate crawler instance with the same collection and archive identities may
resolve it, even with a different provider or physical root; those matching ids
are an explicit claim that both configurations represent the same logical
archive.

ARIs now serve three related purposes:

- safe file inspection through the archive's accessor contract;
- source provenance on emitted and stashed gear nodes;
- unambiguous partial-reindex routing across multiple archives.

The physical `ArchiveDescriptor.root` remains provider configuration. It is not
part of the resource's identity.

## Cache Version Boundary

The former retrieval flow deserialized JSON into the current `Archive` class
and checked `Archive.version()` afterwards. That cannot distinguish incompatible
shapes safely. The repository now performs a streaming preflight over the
top-level object, reads the raw integer version without constructing any domain
object, selects the supported decoder, and only then deserializes the current
shape. This keeps format dispatch at the serialization boundary and avoids a
complete `JsonNode` copy of a potentially large clue archive.

## Incidental Fixes

Re-indexing an archive's own root as a subtree previously failed: relativizing a
path against itself yields a one-element empty path, so both
`ArchiveManager.relativeFolders` and `ArchiveDigger.target` looked for a child
folder named `""`. Both now treat root-equals-path as the root target. This
surfaced through subtree routing in `crawlAll`.

## Consequences for Existing Deployments

Stored clue archives are not compatible: the persisted shape changed and archive
IDs are now per archive rather than per collection. Every deployment re-indexes
once.

## Progress

- [x] Add multi-archive crawler composition and archive-selected operations.
- [x] Collapse archives to exactly one root; remove `ArchiveRoots` and `Bucket`.
- [x] Remove archive locations from `@RetroCollection` and from `Model`.
- [x] Add crawler-wide `crawlAll` with cross-archive Retro ID validation and
      ARI-based subtree routing.
- [x] Add collection-qualified, URI-backed ARIs for inspection and source
      provenance.
- [x] Regroup the gear tree factory, `Stash`, and stats per archive; retain the
      source ARI on each `GearNode`.
- [x] Inspect cache versions before decoding and assign the new shape format 3.
- [x] Demonstrate one shared model with filesystem and ZIP archives.
- [x] Remove the obsolete private smoke-crawl launcher.
- [x] Document the public composition model.
- [x] Run focused and reactor verification.

## Verification

- Canonical `prettify` assertion passed for all remaining changed Java sources.
- `mvn test` passed for the full reactor: 320 tests, 0 failures, 0 errors.
- `mvn clean install` passed for the full seven-module reactor.
