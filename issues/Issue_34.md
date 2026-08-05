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
    shape was introduced as cache format 3.
12. `JsonFileRepository` inspects the raw top-level numeric version before
    deserializing a version-dependent `Archive`. Only a supported version is
    passed to its decoder; missing, malformed, older, and future versions fail
    at the repository boundary. `Archive` pins `version` as its first serialized
    property with `@JsonPropertyOrder`, so the preflight stops on the first
    token instead of scanning the clue tree. The pin is deliberate rather than
    incidental: Jackson would otherwise derive the order from the creator
    signature, which is not a format contract.
13. File resource clues are relative to their artifact, not to the archive
    root. Current local image finders therefore store `front.jpeg` rather than
    repeating the artifact's archive path. Resolution binds the value to the
    artifact source path. Because format 3 values used archive-root-relative
    paths and cannot be distinguished structurally, this semantic change is
    cache format 4 and forces one safe re-index.

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

## Core Model Review

Reshaping the archive level invited a review of the whole chain —
`ArchiveSource` to `Clue` to `Repository` to gear. The clue/fact separation
holds: `GearResolver` derives an effective clue view without mutating the
artifact, and `ClueClassifier` keeps model vocabulary out of the cache, so a
changed model reinterprets a stored archive without re-indexing. The gaps below
are what the review found. Only the resolved cleanups are in this issue's scope;
the open findings are recorded here because this is where they surfaced.

### Resolved here

Three findings were leftovers of the source and archive refactors and were cheap
to close, so they were closed rather than deferred.

- `Clues` was a public record referenced by nothing. Removed.
- `ArchivePath` was never constructed in main or test. It survived only as the
  parameter of two `ArchivePathClueFinder.find` overloads that nothing called,
  superseded by the `ArchiveFolder`/`ArchiveSession` overload the digger uses,
  and it carried a compatibility constructor for a compatibility no longer
  exercised. Meanwhile `Node` models the same rooted-path-with-below-check idea
  and is used. Two types for one concept; `ArchivePath` and both dead overloads
  are gone.
- `Confidence` lived in `archive.clues` but is meaningless to a `Clue`. Only
  `Fact`, `RatedFact`, and matchers use it, so it moved to `gear`. This also
  makes the package boundary state the clue/fact rule instead of blurring it.

### Open: provenance stops at the folder

Design principle 7 promises an answer traceable to its archive, source path,
clues, and facts. `GearNode` carries an `ARI` and `Fact` retains its source
`Clue`, but a `Clue` records only `key` and `value`. It does not know which
`ClueFinder` produced it or which file it came from, so a `FileContentClueFinder`
reading one file inside a folder discards that filename.

`TreeClueFinder` widens the loss: it runs post-order over a subtree and returns
clues for the current folder, so a clue may originate several levels below and be
recorded as observed at the parent. Gathering across those levels is correct —
artifact boundaries are pruned, so the finder stays inside one item's own
material — but the archive keeps no record of how far down a clue actually came
from, and caching makes that permanent. The concern is not that the clue was
collected; it is that the collection cannot afterwards be audited.

An origin on `Clue` — finder identity plus an optional artifact-relative source
path — stays model-independent and therefore does not violate the clue/fact rule,
and it serializes into the existing clue archive.

### Open: the archive has no freshness

Nothing in the archive or stash packages records a timestamp, size, or content
hash. `Archive` holds `{version, id, basePath, root}` with no crawl time, and
`ArchiveEntry` exposes only `path()`.

`ReindexScope.none()` therefore reuses a stored archive indefinitely, and no
component can answer whether the cache still reflects its source. Principle 5
holds that the filesystem archive is the source of truth and stored data is
rebuildable from it; that is true, but nothing can determine *when* rebuilding is
due. Re-indexing is entirely a manual decision.

Making this answerable needs `crawledAt` on `Archive` and optional `size()` and
`lastModified()` on `ArchiveEntry` — optional because a ZIP or a future remote
source may not expose either. That is the precondition for any cheap staleness
sweep.

### Open: an artifact does not know where it is

The digger injects `@id` (a hash of `archiveId::relativePath`) and `@folder` as
synthetic clues, but `Artifact` itself carries no path or ARI; its position is
implied solely by `ArchiveNode` nesting. `GearContext` passes a matcher the gear
type, artifact, and attributes, so a `GearMatcher` cannot make a location-aware
decision and a failure cannot cite a path.

`@id` is a hash of the path where an `ARI` is a resolvable address for the same
thing. Now that ARIs exist, the synthetic id is a candidate for replacement
rather than a fixture.

### Open: the artifact clue-key invariant is unenforced

`Clue` overrides neither `equals` nor `hashCode`, so the `Set<Clue>` inside
`Artifact` deduplicates by identity and two clues may share a key.
`Artifact.jsonGetter` then collects to a map keyed by `Clue::key` and throws
`IllegalStateException` on the duplicate — at stowaway time, after the expensive
crawl that caching exists to avoid repeating.

This is not live on the default path, because `ArchivePathClueFinder.merge` folds
by key first. The invariant lives in a collaborator rather than in the type that
depends on it, and `Artifact(Set<Clue>)` is public. `DuplicateClueException`
exists and is thrown nowhere, which suggests the check was intended on `Artifact`
and never landed.

### Open: a repository cannot forget or enumerate

`Repository` is `stowaway` plus `retrieve`. There is no removal and no listing,
so unregistering an archive leaks its stored entry with no API able to find it,
and nothing can report what a repository currently holds. Both `JsonFileRepository`
and `InMemoryRepository` inherit the gap.

### Open: clue and fact values are unordered

`Clue.value()` is a `Set<String>` and `Fact.value()` a `Set<Object>`, so ordering
is lost and genuine duplicates collapse. Ordered multi-values are the normal case
for retro material — disk sets, volume numbers, multi-part archives.

The set semantics also reach the persisted format: `HashSet` iteration order is
unstable, so stored arrays may reorder between runs and produce noise in a
file-based archive that is meant to be backup- and diff-friendly. `jsonGetter`
additionally writes single-valued clues as scalars and multi-valued ones as
arrays, an asymmetry worth keeping deliberate.

### Not a gap: location is the relation

The review initially recorded folder-shaped gear relationships as a possible
limitation. That was a misreading, and the correction is worth stating because
the rule is currently enforced in code without being written down anywhere.

Gear relation *is* archive location, and this works because the two are
genuinely isomorphic: a physical item can be in exactly one place at a time,
and so can a folder. A CPU sits on one board; a manual sits in one box. Moving
gear therefore means moving folders — fast, pragmatic, and visual, with no
parallel relational model to keep in sync and no chance of a relation
contradicting the archive.

The same reasoning is why RetroCrawler must *not* derive gear type from folder
hierarchy. `Graphics Cards/GeForce 2` must not make the GeForce 2 a graphics
card, because moving that folder would silently change what the item is.
Location is exclusive and therefore safe to read as containment; it is not
safe to read as classification. The tree carries where a thing is, never what
a thing is.

`ArchiveDigger.folderView` implements the boundary: children that already
established an artifact are pruned from the view passed to `TreeClueFinder`s,
so a finder may descend through non-gear subfolders belonging to one item but
can never cross into another piece of gear and absorb its identity. This is
covered by
`ArchiveDiggerTreeClueFinderTest.findsParentCluesPostOrderThroughMetadataFoldersWithoutCrossingArtifactBoundaries`.

The principle was enforced and tested but was not among the design principles in
`AGENTS.md`, and the README presented the pruning as a mechanical property of
tree finders rather than as the rule it protects. That omission was
load-bearing: this review read the entire core and proposed relaxing the
constraint, which is exactly the failure an unstated principle invites. It is
now design principle 9 in `AGENTS.md`, and the pruning filter carries a comment
naming what it protects so it cannot be mistaken for an optimization.

## Consequences for Existing Deployments

Stored clue archives are not compatible: the persisted shape changed and archive
IDs are now per archive rather than per collection. Cache format 4 also changes
file resource values from archive-root-relative to artifact-relative. Every
deployment re-indexes once.

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
- [x] Inspect cache versions before decoding; introduce the new shape as format
      3 and artifact-relative file resource paths as format 4.
- [x] Demonstrate one shared model with filesystem and ZIP archives.
- [x] Remove the obsolete private smoke-crawl launcher.
- [x] Document the public composition model.
- [x] Review the core model from `ArchiveSource` through clues and the
      repository to gear.
- [x] Remove the unused `Clues` record.
- [x] Remove `ArchivePath` and the two dead `ArchivePathClueFinder.find`
      overloads it served.
- [x] Move `Confidence` from `archive.clues` to `gear`.
- [x] Record location-as-relation as design principle 9 and document the
      artifact-boundary pruning it depends on.
- [ ] Follow-up issues for the open review findings: clue provenance, archive
      freshness, artifact location, the artifact clue-key invariant, repository
      removal and enumeration, and value ordering.
- [x] Run focused and reactor verification.

## Verification

- Canonical `prettify` assertion passed for all remaining changed Java sources.
- `mvn test` passed for the full reactor: 320 tests, 0 failures, 0 errors.
- `mvn clean install` passed for the full seven-module reactor.
- Re-verified after the review cleanups removed `Clues` and `ArchivePath` and
  moved `Confidence` to `gear`: `mvn test` passed for the full reactor with 314
  tests, 0 failures, 0 errors. The count dropped with the tests covering the
  removed types; no remaining test needed adjusting, which is the expected
  result for types nothing referenced.
- Re-verified after documenting design principle 9: `mvn test` passed for the
  full reactor with 314 tests, 0 failures, 0 errors, and the canonical
  `prettify` assertion passed for `ArchiveDigger`.
- Re-verified after making file resources artifact-relative: canonical
  `prettify` passed for all 12 affected Java sources, and `mvn clean install`
  passed for the full seven-module reactor with 318 tests, 0 failures, and 0
  errors.
- Note for worktree-based work: `prettify` validates a repository root with
  `Files.isDirectory(repo.resolve(".git"))`, which no Git worktree satisfies
  because its `.git` is a file. The assertion above was obtained by running
  prettify against a reactor copy. Running the canonical formatter inside an
  issue worktree needs a fix in `devtools`.
