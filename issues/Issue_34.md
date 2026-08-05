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
14. The artifact is the deliberate provenance boundary for clues. An artifact
    retains its place in the archive tree, but its clues are a flat set of
    evidence and do not retain finder identity or the exact file or descendant
    folder where each observation originated. This keeps the clue model and
    cache compact and matches the intentional collapsing of subfolder clues.
    File resource values may still carry artifact-relative paths because those
    paths are needed to use the resource; they are not generic clue-origin
    metadata.
15. Every `ArchiveNode` records `crawledAt`, the time of the crawl operation
    that most recently rebuilt that complete subtree. One operation timestamp
    is shared by all nodes rebuilt in a full or multi-subtree reindex. Partial
    replacement preserves timestamps on ancestors and untouched branches, so
    the root timestamp remains the last complete archive crawl. Adding the
    persisted timestamp changes the node shape and advances the cache to format
    5.

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

### Deliberate simplification: provenance stops at the artifact

Design principle 7 promises an answer traceable to its archive, source path,
clues, and facts. That trace deliberately stops at artifact granularity:
`GearNode` carries an `ARI`, `Fact` retains its source `Clue`, and `Clue` records
only `key` and `value`. A clue does not know which `ClueFinder` produced it or
which file it came from.

`TreeClueFinder` runs post-order over a subtree and returns clues for the current
artifact folder, so a clue may originate several levels below. That flattening
is intentional. Artifact boundaries are pruned, so the finder stays inside one
item's own material, while the cache avoids repeating origin metadata for every
observation.

Finder identity and an artifact-relative observation path could be added later
without violating the clue/fact rule, but the current value would mainly be
diagnostic. Until a concrete query or auditing requirement needs that finer
trace, omitting it is a conscious simplification rather than missing provenance.

### Resolved here: every subtree records its crawl time

The original review correctly found that nothing could answer when cached
material was crawled. One timestamp on `Archive` would become misleading after
a partial reindex, because the resulting tree contains material from more than
one crawl operation. The gapless `ArchiveNode` tree is the natural granularity.

`ArchiveNode.crawledAt` records when its complete subtree was last crawled. A
full crawl gives every node the same timestamp. A partial crawl gives every
replacement node one new operation timestamp while structurally rebuilt
ancestors keep their old one. Consequently the root records the last full crawl
and any selected node records the last complete crawl of that subtree.

This records cache age, not freshness. `ReindexScope.none()` continues to mean
unconditional cache reuse. Source fingerprints, modification metadata, and
automatic change detection are deliberately deferred until there is a concrete
need for them.

### Open: an artifact does not know where it is

The digger injects `@id` (a hash of `archiveId::relativePath`) and `@folder` as
synthetic clues, but `Artifact` itself carries no path or ARI; its position is
implied solely by `ArchiveNode` nesting. `GearContext` passes a matcher the gear
type, artifact, and attributes, so a `GearMatcher` cannot make a location-aware
decision and a failure cannot cite a path.

`@id` is a hash of the path where an `ARI` is a resolvable address for the same
thing. Now that ARIs exist, the synthetic id is a candidate for replacement
rather than a fixture.

### Decision: one clue per key is a hard invariant

The original crawler rejected a second clue with the same key. Issue 24
replaced that rule with value merging without an explicit design decision from
the project owner. That change was reversed here.

A clue may contain several values supplied by one authority. Two distinct
clues may not claim the same key within an artifact, even when their values
agree. Merging them would erase the important difference between one source
asserting a multi-valued property and two sources disagreeing about ownership
or value. This is particularly dangerous for collection-valued facts, where a
folder asserting `AGP` and a metadata file asserting `PCI` could otherwise be
accepted as the apparently valid set `{AGP, PCI}`.

Resolution rejects an anonymous observation that is interpreted as a semantic
key already claimed by an explicitly keyed clue. Any number of clue finders may
contribute anonymous clues because those observations claim no semantic key.
Several anonymous clues may later form one multi-valued fact, such as
`[DS] [HD]` or `[schwarz] [weiß, pink]`. A random collision between generated
anonymous keys is not a semantic duplicate; the incoming clue receives a fresh
anonymous key and both observations survive. `Clue` deliberately retains
identity equality so that conflicting observations remain visible long enough
to be rejected rather than being silently discarded by a `Set`.

### Decision: `Clues` carries the invariant, `Set<Clue>` cannot

The invariant was first restored by enforcing it at every boundary that handled
clues. That left `Set<Clue>` as the currency between finders, the crawler, and
`Artifact`, which was wrong twice over.

It said the wrong thing. Because `Clue` keeps identity equality on purpose, a
set promises a uniqueness it never enforces, and it cannot express the rule that
actually applies, which is one clue per *key*. The invariant consequently had to
live in whichever collaborator happened to hold the set, and every reader of a
`Set<Clue>` had to know that.

It also made the same collection prove itself repeatedly. `ArchivePathClueFinder`
opened a fresh accumulator per finder result and re-added everything gathered so
far; `enrich` re-checked the set `find` had just returned; `ArchiveDigger` then
dropped to a plain `HashSet` to add the synthetic clues, which checked nothing;
and `Artifact` re-checked the lot. That last pass was the only one that could
catch a synthetic clue colliding with a found one, so the redundancy was
accidentally load-bearing in exactly one place.

`Clues` replaces the set: immutable, in observation order, one clue per key, and
constructible only through a `ClueAccumulator`. The accumulator is the single
place a clue is ever inspected. Each observation is checked once, as it arrives,
against everything observed so far, and nothing downstream inspects it again.
Seeding an accumulator from existing `Clues` is a copy rather than a second
inspection.

This is a breaking change to the clue-finder SPI: `FolderNameClueFinder`,
`FileNameClueFinder`, `FileContentClueFinder`, and `TreeClueFinder` return
`Clues`, and `Artifact` takes and returns `Clues`. A finder that emitted two
clues for one key is now rejected in the finder itself rather than one layer
later. `Clues` also preserves observation order end to end; the previous
`Set.copyOf` calls discarded the ordering that `LinkedHashMap` and
`LinkedHashSet` had been carefully building, which is the value-ordering finding
recorded below.

### Decision: a rejection has to say where

Restoring the invariant made clue finding throw where it used to merge, and the
failure named nothing: not the folder, not the finder, not the file. A crawl over
a real archive reported `Duplicate clue key 'bus'` and left the cataloguer to
find the folder by hand.

Position is known in three layers, and no single place knows all of them. The
digger knows the archive-relative folder. `ArchivePathClueFinder` knows which
finder ran and what it was reading. Only the finder can know where inside that
source, because it normalizes what it reads — `[trw 10510]` becomes key
`theRetroWebId`, so the raw text is no longer searchable for the resulting clue.

`ClueFindingException` therefore collects context in layers, each producing a new
exception rather than mutating one in flight, and renders a compiler-style
`path:line:column: who` header. It wraps whatever the finder threw and keeps it
as the cause, so a caller that cares which condition occurred can still ask.
A finder reports a position by passing a `ClueLocation` when it accumulates a
clue; `ClueAccumulator` remembers a sighting per accepted clue so a duplicate is
drawn against *both* observations rather than only the second one.

Positions do not live on `Clue`: they are crawl-time diagnostics, not evidence,
and would reach the cached JSON. They *are* carried by `Clues`, because a finder
accumulates privately and hands its work back — without that, every position a
finder tracked would die at the return statement, and the common conflict
between a folder name and `retro.md` could only be reported by source.

The first attempt kept `Clues` free of them and accepted exactly that loss,
justified by the memory a position would cost in every cached artifact. That
justification was wrong: `Artifact` can simply drop them, which it now does, so
positions live for one folder's crawl — bounded by tree depth, not archive size —
just as they would have anyway. What the argument was really protecting was the
tidiness of a type that had just been cleaned up, which is not worth the feature.

The artifact is the right place to stop. A retrieved archive has no folder name
or document left to point into, so a position that crossed the cache would
describe text nobody read this run, and diagnostics would differ depending on
whether an archive came from a crawl or from the repository.

Resolution-time conflicts stay a `DuplicateClueException` rather than becoming a
finding failure. When `[AGP]` in a folder name meets `bus: PCI` in `retro.md`,
the archive was crawled cleanly and only the model's vocabulary reveals the
competition; the phase differs, so the type does. `RetroCrawlerImpl` gives it the
same header naming the artifact.

Failure remains fail-fast for now. Collecting clue failures and reporting them
together is the better fit for cataloguing a messy archive — a compiler has hard
rules too and still reports every error in one pass — but it changes the crawl
contract and is left to its own issue.

### Decision: metadata-folder status is established, never inferred

Collecting failures instead of aborting introduces a folder state that does not
exist today: read, but not understood. The artifact-boundary pruning that
protects design principle 10 asked `child.node().artifact() == null`, which
answers "does this child carry an artifact" and not "did the crawl establish
that this child is metadata". Those coincide only because an unreadable folder
aborts the whole dig.

Under collection they diverge, and the failure mode is worse than a lost cache.
A folder that failed would have no artifact, so it would no longer be pruned,
and its parent's tree finder would descend into a folder holding another item's
evidence and absorb its identity. The parent then produces different clues than
it should, so the crawl report itself fills with findings the recovery caused
and real ones may be masked. Under fail-fast this cannot happen, because the
parent is never built.

`FolderOutcome` now answers the question once. It began as an enum carrying a
boolean, which still left `DigResult` holding a folder view that was meaningless
for a child that had established an artifact — the view was carried up and then
discarded by a filter. An invariant guarded by a filter someone must remember is
the same shape as the `Set<Clue>` problem, so the type absorbed it: a sealed
`FolderOutcome` whose `MetadataFolder` carries the readable view and whose
`EstablishedArtifact` carries nothing. A folder that is another item's evidence
has no view to hand up, so an ancestor cannot read one by mistake and pruning
stops being a check at all. `DigResult` is now `(node, outcome)`.

Sealing it also strengthens the forcing function. An enum constant obliged a new
state to answer a boolean in the constructor; a sealed type stops every
exhaustive switch compiling until the new case is handled. Verified by adding a
`Failed` record, which fails compilation with "switch does not cover all
possible input values" — exactly the reminder the crawl-report work will want.

This changes no behaviour today; it removes the coincidence that would have made
the change dangerous. The behavioural rule is covered by
`ArchiveDiggerTreeClueFinderTest.findsParentCluesPostOrderThroughMetadataFoldersWithoutCrossingArtifactBoundaries`,
confirmed by mutating the earlier filter to accept every child.

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
now design principle 10 in `AGENTS.md`, and the pruning filter carries a comment
naming what it protects so it cannot be mistaken for an optimization.

## Consequences for Existing Deployments

Stored clue archives are not compatible: the persisted shape changed and archive
IDs are now per archive rather than per collection. Cache format 4 also changes
file resource values from archive-root-relative to artifact-relative, and format
5 timestamps every archive node. Every deployment re-indexes once.

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
      3, artifact-relative file resource paths as format 4, and archive-node
      timestamps as format 5.
- [x] Demonstrate one shared model with filesystem and ZIP archives.
- [x] Remove the obsolete private smoke-crawl launcher.
- [x] Document the public composition model.
- [x] Review the core model from `ArchiveSource` through clues and the
      repository to gear.
- [x] Remove the unused `Clues` record.
- [x] Remove `ArchivePath` and the two dead `ArchivePathClueFinder.find`
      overloads it served.
- [x] Move `Confidence` from `archive.clues` to `gear`.
- [x] Record location-as-relation as design principle 10 and document the
      artifact-boundary pruning it depends on.
- [x] Define the artifact as the deliberate clue-provenance boundary.
- [x] Timestamp every archive subtree and preserve crawl history across partial
      reindexing.
- [x] Restore one clue per explicit key, reject explicit-versus-anonymous
      semantic competition, and preserve arbitrary anonymous clues from any
      number of finders.
- [x] Replace `Set<Clue>` with `Clues` across the clue-finder SPI, the crawler,
      and `Artifact` so the invariant lives in the type, each clue is inspected
      once where it is observed, and observation order survives.
- [x] Report every clue failure against its archive location, with a
      finder-supplied position where the finder tracks one.
- [x] Make transparency to tree finders an established outcome rather than an
      inferred one, so collecting clue failures cannot silently reopen an
      artifact boundary.
- [ ] Follow-up issues for the remaining open review findings: artifact
      location, repository removal and enumeration, and collecting clue failures
      into one crawl report instead of failing fast.
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
- Re-verified after documenting design principle 10: `mvn test` passed for the
  full reactor with 314 tests, 0 failures, 0 errors, and the canonical
  `prettify` assertion passed for `ArchiveDigger`.
- Re-verified after making file resources artifact-relative: canonical
  `prettify` passed for all 12 affected Java sources, and `mvn clean install`
  passed for the full seven-module reactor with 318 tests, 0 failures, and 0
  errors.
- Re-verified after timestamping archive nodes: canonical `prettify` passed for
  all 7 affected Java sources, and `mvn clean install` passed for the full
  seven-module reactor with 319 tests, 0 failures, and 0 errors.
- Re-verified after restoring the clue-key invariant: canonical `prettify`
  assertion passed for all 10 affected Java sources, and `mvn clean install`
  passed for the full seven-module reactor with 324 tests, 0 failures, and 0
  errors.
- After the corresponding devtools update, canonical `prettify` ran directly
  in the linked issue worktree. `--apply --select uncommitted` formatted the
  one remaining source and the matching assertion passed for all 11 selected
  Java sources without the previous scratch-repository workaround.
- A clean Issue 34 release was installed in the private NAS runtime and used
  for a fresh isolated full crawl spanning all three collection archives. The
  restored invariant stopped extraction in the first archive at approximately
  188 of 517 crawl regions: one folder name caused `BracketClueFinder` to emit
  two explicit clues with key `1`. The failure occurred entirely within the
  folder-name finder, before file clues, resolution, or cache stowaway. No
  partial cache or collection change resulted. This validates the hard failure
  behavior and exposes a concrete bracket-language conflict for collector
  review.
