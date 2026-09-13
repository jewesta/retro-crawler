# Archives and resources

RetroCrawler separates an Archive's public identity from the provider that
makes its hierarchy readable. This document covers providers, Archive Resource
Identifiers, inspection, crawling, reindexing, and crawl observations. Start
with the [README](../README.md) for the project overview.

## Archive sources

Traversal is supplied by an application-selected `ArchiveSource`.
`FileSystemArchiveSource` is the default and uses the NIO filesystem
associated with an Archive's root `Path`.

Every crawler registers at least one Archive. Clue finders, Fact parsers, and
Gear resolution are shared by the collection model, while each Archive is
paired with the provider exposing its root:

```java
ArchiveDescriptor myCollection = ArchiveDescriptor.of(
        ArchiveId.of("my_collection"), Path.of("my-collection"));
ArchiveDescriptor museumCollection = new ArchiveDescriptor(
        ArchiveId.of("museum_collection"),
        "Museum collection",
        Path.of("museum"));
ArchiveDescriptor incomingMaterial = new ArchiveDescriptor(
        ArchiveId.of("incoming_material"),
        "Incoming material",
        Path.of("incoming.zip"));

RetroCrawler crawler = RetroCrawler.builder()
        .model(retroHardwareModel)
        .repository(repository)
        .archive(myCollection)
        .archive(museumCollection, sshArchiveSource)
        .archive(incomingMaterial, new ZipArchiveSource())
        .build();
```

`archive(descriptor)` selects the filesystem provider. A provider-specific
root is a crawl coordinate, not part of the collection model.

## ZIP archives and provider sessions

ZIP Archives can be crawled directly without extraction. Select
`ZipArchiveSource` and use the local ZIP file as the provider root. Entry names
become descendant source paths, and folders omitted from the ZIP directory are
inferred from their children.

A provider session supplies its root folder and classified direct listings
while RetroCrawler retains control of planning and depth-first traversal. File
content is optional. When it is available, the session invokes a generic
`ArchiveFileAccessor` synchronously and closes the supplied `InputStream`
before returning the result. An empty result means content was unavailable;
the finder sees `Optional.empty()` and can continue without a content-derived
Clue.

## Archive Resource Identifiers

An Archive Resource Identifier (`ARI`) addresses a public collection resource
without exposing a physical root or provider detail. It contains the
collection id, Archive id, and Archive-relative path:

```text
ari:/retro_pc_demo/incoming_material/Graphics%20Cards/Voodoo%203/front.jpg
```

Gear trees, Stash nodes, and resource-valued Gear Facts carry ARIs directly:

```java
ARI imageSource = gear.getFrontImage().orElseThrow();
Optional<byte[]> image =
        crawler.inspect(imageSource, InputStream::readAllBytes);
```

The crawler rejects an ARI from another collection or an unknown Archive.
Another crawler configured with the same collection and Archive identities
may resolve the resource through a different provider and root.

The crawler closes the short-lived provider session as well as the content
stream. `Optional.empty()` means the provider recognizes the file but does not
expose its content. A missing address or one naming a folder raises
`NoSuchFileException`.

Provider paths remain internal crawl coordinates and need not be locally
accessible. The ARI is the application-facing resource identity.

## Clue sources and cached references

Clue finders receive resource identity directly through
`ArchiveFolderView.ari()` and `ArchiveFileView.ari()`. The Archive definition
derives it from the collection identity, Archive descriptor, and relative
resource path. A finder therefore never needs the physical root to record
provenance.

When a file-name Clue refers to a resource belonging to an Artifact, its raw
cached path is relative to that Artifact rather than to the Archive root. A
direct `front.jpeg` is stored as `front.jpeg`; a resource below the Artifact
may be stored as `Box/front.jpeg`. `ARIParser` resolves that observation
against the Artifact ARI during Gear resolution. The resulting Fact remains
independent of the active provider and root.

The JSON cache likewise uses the Archive tree as context instead of repeating
complete ARIs on every Clue. The collection namespace is stored once per
Archive; a source equal to its Artifact is stored as `.` and a descendant as
an Artifact-relative `./...` path. Retrieval expands both forms back into full
ARIs before constructing the Clue.

A declared source must be an exact member of the pruned folder view supplied
to its finder. Consequently the cache has no full-ARI fallback for Clue
sources: every stored source is contextual and within the readable evidence
boundary.

## Access and crawling

Crawling produces one complete immutable Stash spanning every configured
Archive. `access` returns the parked Stash or resolves stored clue Archives on
first access, physically crawling only those whose stored clues are missing.
An explicit `crawl` rereads the requested scope and parks its result only after
the complete candidate succeeds:

```java
Journal journal = new Journal();

Stash stash = crawler.access(journal);

Stash recrawled = crawler.crawl(
        new Journal(), ReindexScope.subtree(changedShelf));
```

A partial reindex replaces the requested subtree while preserving untouched
branches and other Archives from their stored clue Archives. The request is
routed by ARI rather than by a provider path.

## Statistics and crawl observations

Structural statistics are calculated from an immutable Stash on request and
are not persisted as another representation of the collection. Crawl times
are different: they are observations made during physical crawling and
carried from the persisted clue Archive into the Stash:

```java
StashStats stats = stash.stats();

Optional<Instant> completeCrawlStarted = stash.crawlStartedAt(archiveId);
Optional<Instant> completeCrawlObserved = stash.observedAt(archiveId);
Optional<Duration> completeCrawlDuration = stash.crawlDuration(archiveId);

Optional<Instant> shelfCrawlStarted = stash.crawlStartedAt(changedShelf);
Optional<Instant> shelfObserved = stash.observedAt(changedShelf);

List<ArchiveCrawlTimes> timesByArchive = stash.crawlTimes();
```

Every persisted Archive node records when its complete subtree was last
crawled. Nodes rebuilt by one full or multi-subtree operation receive the same
`crawlStartedAt`; each receives its own `observedAt` after the folder and its
children have been inspected. The root observation is therefore the end of a
complete Archive crawl and allows its duration to be calculated without
storing another value.

Partial reindexing preserves ancestor and untouched-branch observations. A
newer subtree observation records a later partial crawl while the Archive root
continues to describe the last complete crawl. These values describe cache
age; RetroCrawler does not claim that the physical source is still unchanged.

`ArchiveCrawlTimes.observations()` exposes indexed folder ARIs in Archive tree
order. See [Repositories and operations](operations.md) for persistence,
progress, cancellation, and failure behavior.
