## ![RetroCrawler AI Slop Logo](docs/images/retro_crawler_ai_slop_logo.png)

**RetroCrawler** is a Java framework for *structurally crawling* directory-based archives and turning them into typed domain objects so you can manage your stash of retro gear.

If you are like us then you have your collection organized as files and folders. This is simple, pragmatic and backup-friendly. Because of this, RetroCrawler is designed specifically for collections that were **not originally structured as databases** — such as retro computer hardware documentation (pictures, manuals, drivers), software archives, ROM libraries or document repositories.

RetroCrawler does not require specific schemas, metadata files, or folder layouts.
Instead, you can use your own personal already existing folder structure, provide context via `ClueFinder`s, `FactParser`s and `GearMatcher`s and RetroCrawler **infers structure from context** using a two-phase pipeline.

---

## Core Concepts

### Archive
A separately identified, hierarchical collection holding, rooted at exactly one
place. The default source is a local directory tree, but providers may expose
ZIP entries, remote files, or other file-like hierarchies through the same
archive model.

One `RetroCrawler` applies a shared model to every archive registered with it.
Each archive keeps its own identity, root, source provider, repository entry,
and crawl lifecycle, so a collection spread across several disks, mounts, or
media is composed as several archives rather than as several roots of one.
Every access or crawl resolves a complete Stash across those archives and
validates Retro ID uniqueness across all of them.

### Artifact
An optional representation of a single folder in the archive.
An artifact exists only if meaningful information can be extracted from that folder which we call `Clue`s. This is a raw representation of a single **potential** piece of your collection.

### Clue
A raw key–value observation derived from:
- folder names
- file names
- file contents

Clues are always **string-based** and may contain multiple values. This is a raw representation of a **potential** property of a piece in your collection.

Every crawled clue retains the unique simple class name of the finder that
produced it. A finder may also declare the exact archive resources that
contributed to an individual clue. Those sources are stable `ARI`s, never
physical paths. The list is optional: no declared source means that the finder
makes no resource-level provenance claim, and merely accessing a resource never
causes RetroCrawler to infer one.

Within one artifact, every clue key has exactly one authority. One clue may
contain several values, but separate clues from different finders must not
claim the same explicit key. An anonymous observation must likewise not compete
with an explicitly keyed clue after resolution discovers its meaning.
RetroCrawler rejects that archive inconsistency even when the values agree; it
never merges observations from separate authorities into one clue.

Anonymous clues are deliberately different: they claim no semantic key. Any
number of clue finders may contribute anonymous clues to one artifact, and all
of those observations survive under distinct generated keys. Resolution may
later interpret several of them as values of the same fact.

### Clues
The clues observed at one archive location: immutable, in observation order, and
holding exactly one clue per key. A clue finder returns `Clues` and an `Artifact`
holds `Clues`, so the one-authority rule is carried by the type rather than
re-checked at each boundary. Build them with `Clues.of(...)`, or accumulate them
one observation at a time:

```java
final ClueAccumulator clues = Clues.accumulator();
clues.add(folder.clue("bus", "AGP"));
clues.add(folder.clue("Example Graphics Board"));
return clues.clues();
```

`ArchiveFolderView` and `ArchiveFileView` carry their authoritative ARIs. Their
`clue(...)` factories attach that resource to the new clue. A finder that does
not want to make an exact source claim uses `Clue.of(...)` instead. Sources for
an aggregate clue are added deliberately while the finder loops over its
contributors; there is no bulk operation that assigns every accessed resource
to every returned clue.

A second clue claiming a key already taken is rejected with a
`DuplicateClueException` right where it is observed.

### Clue diagnostics
Because RetroCrawler rejects a conflict instead of merging it, a failed crawl
has to say where. Every clue failure leaves a crawl as a `ClueFindingException`
with a compiler-style header naming an authoritative resource ARI and the
finder. A failure inside `ArchiveFileView.peek(...)` names that exact file;
otherwise the candidate folder is the accurate default. The original condition
stays available as the cause.

A finder that tracks offsets can hand them over, and the rejection then points at
the tag you actually wrote:

```
ari:/my_collection/hardware/Graphics%20Cards/Example%20Board: BracketClueFinder failed while finding clues.
Duplicate clue key 'bus'. One artifact may contain only one clue for a key. First values: [ISA], duplicate values: [PCI].
  Example Board [bus ISA] [200001] [bus PCI]
                ^^^^^^^^^ first
                                   ^^^^^^^^^ duplicate
```

Pass a `ClueLocation` when you accumulate:

```java
clues.add(Clue.of(key, values), ClueLocation.in(folderName, openingBracket, length));
```

A finder that reports no exact source still produces the header. When the two
conflicting clues come from *different* finders, their durable finder and source
provenance are both reported — text positions travel with the `Clues` a finder
hands back:

```
ari:/my_collection/hardware/Graphics%20Cards/Example%20Board: RetroMarkdownClueFinder failed while finding clues.
Duplicate clue key 'bus'. One artifact may contain only one clue for a key. First values: [AGP], duplicate values: [PCI].
  The first clue was observed by BracketClueFinder from ari:/my_collection/hardware/Graphics%20Cards/Example%20Board, line 1, column 15.
    Example Board [bus AGP]
                  ^^^^^^^^^
  The duplicate clue was observed by RetroMarkdownClueFinder, line 3, column 1.
    bus: PCI
    ^^^
```

Positions stop at the artifact. A retrieved clue retains source ARIs but no
snapshot of the source text, so a cached offset could point into content that
has since changed.

The JSON cache uses the archive tree as context rather than repeating complete
ARIs on every clue. The collection namespace is stored once per archive; a
source equal to its artifact is stored as `.` and a source below it as an
artifact-relative `./...` path. Retrieval always expands the stored form back
into full ARIs before constructing the clue.

A finder may cite only the candidate folder or a resource present in its exact
pruned `ArchiveFolderView`. A path merely being below the artifact is not
enough: a pruned child artifact is outside the readable evidence boundary. If
any returned clue declares another source, that finder's complete result is
rejected before accumulation. Consequently the cache has no full-ARI fallback
for clue sources; every stored source is contextual.

### Gear
A user-defined domain object created from a set of facts. This is an **identified**, real piece in your collection.
Gear types are **not** required to implement framework interfaces and require only a no-arg constructor. It's "bring your own type".

### Fact
A typed interpretation of a clue.
Facts are produced by user-defined parsers and may be any Java type.

---

## Processing Pipeline

RetroCrawler operates in two distinct phases:

### 1. Archive / Clue Phase
The archive is traversed recursively.
For each folder, registered `ClueFinder`s extract clues and produce an `Artifact`.

The extracted clue archive is stowed away through an application-selected `Repository` so that expensive rescans can be avoided. The bundled `JsonFileRepository` uses JSON files on local storage. Once a scan is done, queries on the archive are blazingly fast. If you restart your app, the archive is quickly retrieved from the repository.

Every finder implements the same small contract and runs once per candidate
folder, after its children have been classified. It receives an
`ArchiveFolderView` containing the candidate's name, its direct files, and only
those child folders that were positively established as clue-free metadata
folders. Child artifacts and failed folders are structurally absent, so a
finder cannot cross into another potential collection part.

A finder decides which parts of that view matter. It may use the folder name,
enumerate files and metadata subfolders, and inspect file content lazily through
`ArchiveFileView.peek(...)` when the source exposes it. Finders are independent:
several may inspect the same file, while the one-authority-per-clue-key rule
governs what they are allowed to return. Every folder and file view has an ARI;
using its `clue(...)` factory records that exact source, while `Clue.of(...)`
deliberately records none.

### 2. Gear / Fact Phase
All known clues are converted into facts using registered parsers.
Based on these facts, `GearMatcher`s determine which gear type best represents an artifact.
The corresponding `GearFactory` then creates the final domain object.

Unknown or unparseable clues are preserved and may be accessed explicitly.

---

## Configuration via Annotations

RetroCrawler's collection and gear model can be configured via annotations:

- `@RetroCollection`
  Declares the collection identity, optional working directory, and
  `ArchivePathFilter`. Exactly one collection is present in a model. Archive
  roots and providers are runtime crawler configuration.

- `@RetroClues`
  Declares the ordered `ClueFinder`s that inspect each candidate's pruned
  archive view and produce crawl-time clues. Each finder must be a named class
  with a simple name unique within the configured list, because that compact
  name is retained as durable clue provenance. Registering the same finder
  twice and using two classes with the same simple name are both rejected.

- `@RetroGear`
  Declares a gear type and its matcher.

- `@RetroAnyGear`
  Declares the optional fallback gear type produced when no matcher recognizes
  an artifact or equally confident matches cannot be resolved through their
  type hierarchy. At most one fallback may be present in a model.

- `@RetroFact`
  Declares how a field is populated from a clue.

- `@RetroFactCatalog`
  Optionally overrides the catalog used by a catalog-backed fact parser. It
  does not select or instantiate that parser.

- `@RetroAnyAttribute`
  Captures all remaining unassigned facts. Especially useful on `@RetroAnyGear`
  fallback types.

This allows the framework to remain strongly typed while requiring minimal boilerplate.

Source noise can be pruned before entries are supplied to clue finders. The
bundled opt-in filter ignores every dot-prefixed entry and
common operating-system or NAS service entries such as `Thumbs.db`,
`__MACOSX`, `@Recycle`, and `System Volume Information`:

```java
@RetroCollection(
        id = "my_collection",
        pathFilters = {
                IgnoreDotPaths.class,
                IgnoreWindowsSystemPaths.class,
                IgnoreMacSystemPaths.class,
                IgnoreLinuxSystemPaths.class,
                IgnoreQNAPSystemPaths.class,
                IgnoreSynologySystemPaths.class
        })
```

An entry must be accepted by every configured filter; an empty list accepts
everything. Applications can implement `ArchivePathFilter` with a public
no-argument constructor for annotation use, or inject configured instances
through `Model.Builder.pathFilters(...)`. Rejecting a folder prunes its
entire subtree during both crawl planning and digging. Changing the filters
requires a re-index because retrieved clue archives retain the result of their
original crawl.

---

## Model Discovery

RetroCrawler discovers annotated archive and gear types recursively below an application's base package:

```java
Model model = Model.from("com.example.collection");
```

Applications that need deterministic or custom discovery can instead provide a `Set<Class<?>>` or `TypeSource`.

A model declares how a collection is interpreted, never where it is stored.
Archive roots are deployment configuration and are registered on the crawler.

Deployment-specific settings can override annotation defaults while the model
is built:

```java
Model model = Model.builder()
        .typesFrom("com.example.collection")
        .workingDirectory(Path.of("retro-work"))
        .factCatalog(MyCatalogParser.class,
                configuration -> configuration.catalogFile("my-catalog.tsv"))
        .build();
```

External parser catalogs resolve below the working directory's `catalogs`
folder. Configuring a parser does not manifest it: a catalog is loaded only
when a discovered field-level `@RetroFact` actually selects that parser.

Catalogs use one strict UTF-8 TSV format. Any number of blank or `#` comment
lines are allowed. The first data line contains enum constant names as headers;
all keys must occur exactly once, while their order is arbitrary.

---

## Archive Sources

Archive traversal is provided by an application-selected `ArchiveSource`.
`FileSystemArchiveSource` is the default and uses the NIO filesystem associated
with the archive's root `Path`.

Every crawler registers at least one archive. The model's clue finders, fact
parsers, and gear resolution are shared, while each archive is paired with the
provider that exposes its root:

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

`archive(descriptor)` selects the filesystem provider.

ZIP archives can be crawled directly without extracting them. Select
`ZipArchiveSource` and configure the archive root as the path of a local ZIP
file. The ZIP path is the provider root. Entry names become descendant source
paths, and folders omitted from the ZIP directory are inferred from their
children.

A session supplies its root folder and classified direct listings while
RetroCrawler retains control of planning and depth-first traversal. File
content is optional. When available, the session invokes a generic
`ArchiveFileAccessor` synchronously and closes the supplied `InputStream`
before returning its result. An empty result means that content was not
available. A finder sees that as an empty `Optional` and can continue without a
content-derived clue.

Public resources are addressed by an Archive Resource Identifier (`ARI`). An
ARI contains the collection id, archive id, and archive-relative resource path,
but no physical root or provider details:

```text
ari:/retro_pc_demo/incoming_material/Graphics%20Cards/Voodoo%203/front.jpg
```

Gear trees, `Stash` nodes, and resource-valued Gear facts carry ARIs directly:

```java
ARI imageSource = gear.getFrontImage().orElseThrow();
Optional<byte[]> image = crawler.inspect(imageSource, InputStream::readAllBytes);
```

The crawler rejects an ARI from a different collection or an unknown archive.
Another crawler configured for the same collection and archive identities may
resolve it through a different root or provider. The crawler closes the
short-lived session as well as the content stream. `Optional.empty()` means the
provider recognizes the file but does not expose its content; a missing or
folder address raises `NoSuchFileException`.

Provider paths remain internal crawl-time coordinates; providers must not
require them to be locally accessible. ARIs are the application-facing resource
identity.

Clue finders receive that identity directly through `ArchiveFolderView.ari()`
and `ArchiveFileView.ari()`. The archive definition derives each one from its
collection identity, archive descriptor, and archive-relative resource path.
Finders never need the physical archive root to record provenance.

When a file-name clue refers to a resource belonging to an artifact, its cached
path is relative to that artifact rather than to the archive root. A direct
`front.jpeg` is therefore stored as `front.jpeg`; a resource below the artifact
may be stored as `Box/front.jpeg`. `ARIParser` resolves that raw observation
against the artifact ARI during Gear resolution. The resulting fact therefore
remains independent of the current provider and physical archive root.

Every persisted archive node records when its complete subtree was last
crawled. All nodes rebuilt by one full or multi-subtree operation receive the
same timestamp. Partial reindexing preserves the timestamps of ancestors and
untouched branches, so the root timestamp remains the time of the last complete
archive crawl. These timestamps record cache age; RetroCrawler does not
currently attempt automatic source-change detection.

Crawling always produces a complete immutable `Stash` spanning every configured
archive. `access` returns the parked Stash, or resolves stored clue archives on
first access and physically crawls only archives whose stored clues are missing.
An explicit `crawl` physically rereads the requested scope and parks its result
only after the complete candidate succeeds:

```java
Journal journal = new Journal();

Stash stash = crawler.access(journal);

Stash recrawled = crawler.crawl(
        new Journal(), ReindexScope.subtree(changedShelf));
```

Structural statistics are calculated from an immutable Stash when requested;
they are not stored as another representation of the collection. Crawl times
are different: they are observations recorded during the physical crawl and
carried from the persisted clue archive into the Stash:

```java
StashStats stats = stash.stats();

Optional<Instant> completeCrawlStarted = stash.crawlStartedAt(archiveId);
Optional<Instant> completeCrawlObserved = stash.observedAt(archiveId);
Optional<Duration> completeCrawlDuration = stash.crawlDuration(archiveId);

Optional<Instant> shelfCrawlStarted = stash.crawlStartedAt(changedShelf);
Optional<Instant> shelfObserved = stash.observedAt(changedShelf);

List<ArchiveCrawlTimes> timesByArchive = stash.crawlTimes();
```

`ArchiveCrawlTimes.observations()` exposes every indexed folder ARI in archive
tree order. All nodes produced by one operation share its `crawlStartedAt`;
each node receives its own `observedAt` after that folder and its children have
been inspected. The root is therefore the final observation of a complete
archive crawl, allowing its duration to be calculated without storing another
value. A newer subtree observation records a later partial crawl while
untouched nodes retain their earlier observations. These are crawl times, not
a claim that the physical source is currently unchanged.

A `Stash` contains every recognized Gear in its natural archive hierarchy. A
typed immutable query is materialized as a lifted `Batch<G>`:

```java
Batch<RetroHardware> working = stash.query(RetroHardware.class)
        .where(museumCollection.id())
        .where(RetroHardware::isWorking)
        .pull();

Batch<RetroHardware> agpCardsAmongEverythingElse = stash.query(RetroHardware.class)
        .whereIf(GraphicsCard.class, card -> card.bus() == Bus.AGP)
        .pull();
```

`whereIf` applies its predicate only to the named Gear type. Other selected
Gear remains in the Batch. A Batch retains its archive groups through
`archives()` and exposes their cumulative forest through `roots()`. Its flat
`gear()` view is derived lazily and cached.

Every `GearNode` produced by RetroCrawler also carries one focused
`ResolutionTrace`. The trace retains the source Artifact, detection and final
attribute snapshots, every Gear matcher decision, the selected match, and
non-fatal ambiguities:

```java
GearNode<RetroHardware> node = working.roots().getFirst();
ResolutionTrace trace = node.trace().orElseThrow();

List<Fact> factsUsedToBuildGear = trace.resolved().facts();
List<ResolutionTrace.Match> consideredTypes = trace.matches();
```

Detection and final attributes remain separate because contextual Facts become
eligible only after a Gear type has been selected. Queries preserve the trace
while lifting nodes into a typed Batch. Programmatically constructed
`GearNode`s may have no trace. Traces are resolved Stash state; they are neither
written to the clue cache nor injected into user Gear objects. Fatal resolution
failures that produce no GearNode remain in the operation `Journal`.

Every semantic Fact key also contributes exactly one structured
`FilterDefinition`, even when several Gear types declare that Fact. The
definition records its value type, cardinality, applicable Gear types, and the
non-optional `FilterType` supplied by the effective `FactParser`. Built-in
string parsers describe text filters, enum parsers describe ordered choices,
integer and temporal parsers describe ranges, and an ordinary custom parser
defaults to exact equality.

`crawler.filters()` exposes the complete model vocabulary. For a choice
filter, that includes every conceivable declared option. A Stash or Batch
exposes the same definitions and lazily computes and caches their observed
availability. For example, after selecting the typed `busFilter` definition
from that list:

```java
FilterAvailability.Choices<?> available =
        (FilterAvailability.Choices<?>) stash.availability(busFilter);

List<? extends FilterAvailability.Option<?>> options = available.options();
Batch<RetroHardware> agp = stash.query(RetroHardware.class)
        .where(busFilter, ExpansionBus.AGP)
        .pull();
```

On a Stash or Batch, `filters(FilterSelection.ALL)` returns that complete model
vocabulary, while `filters(FilterSelection.RELEVANT)` retains only definitions
with at least one populated Gear occurrence in that particular data set.
Zero-argument `filters()` remains the shorthand for all definitions.

Each choice option remains in `options()` when absent; its
`matchingOccurrences()` is then zero and `present()` is false. Counts describe
Gear occurrences rather than raw repeated values. A Fact criterion applies to
all Gear types that declare its key, rejects applicable Gear with no matching
value, and retains Gear types to which the Fact does not apply. Batch
availability is recomputed over the materialized result, so a UI can update its
facets after a pull without losing globally conceivable options.

Every `GearNode` retains the ARI of the artifact that produced it. Complete
Stash construction validates Retro ID uniqueness across all registered
archives. A subtree crawl is routed by its ARI; other archives reuse their
stored clue archives.

---

## Optional Shared Model

RetroCrawler remains a bring-your-own-type framework. The optional
`retro-crawler-model` module provides reusable value types and canonical
parsers for facts whose meaning is stable across collections, including ISBNs,
MAC addresses, category-qualified The Retro Web references, expansion buses,
memory vocabulary, and data capacities.

Collection adapters remain responsible for mapping their own folder tags,
metadata files, or other clues onto that shared vocabulary. The core does not
depend on the shared model.

---

## Archive Repository

Applications must explicitly select a `Repository`. `JsonFileRepository` is a
convenient persistent implementation and uses the local `cache` directory when
constructed without a path:

```java
Model model = Model.from("com.example.collection");
Repository repository = new JsonFileRepository(Path.of("my-cache"));

RetroCrawler crawler = RetroCrawler.builder()
        .model(model)
        .repository(repository)
        .archive(ArchiveDescriptor.of(
                ArchiveId.of("my_archive"), Path.of("my-archive")))
        .build();
```

For applications that only need the extracted archives for the lifetime of the
process, the framework also supplies a thread-safe `InMemoryRepository`:

```java
Repository repository = new InMemoryRepository();
```

An in-memory repository does not write to the filesystem and starts empty after
an application restart.

A missing stored archive causes the configured source to be crawled. If a
stored archive cannot be retrieved, RetroCrawler reports the repository failure
and rebuilds it from that source.

JSON cache format version 7 is inspected before the stored payload is
deserialized. Unsupported, missing, or malformed versions are rejected at the
repository boundary so an incompatible payload is never parsed as the current
`Archive` shape. Artifact-relative clue sources are also checked for traversal;
they cannot escape the artifact that supplies their context.

---

## Journal, Progress, and Failure Handling

Crawler operations accept an operation-scoped `Journal`. The journal owns the
progress lifecycle together with the failure record. Its underlying
`Progressor` remains a neutral progress mechanism, while immutable structured
progress is exposed read-only through `journal.progress()`. A lightweight
message view is available for simple command-line or GUI integrations:

```java
Progressor progressor = Progressor.reportingMessages(System.out::println);
Journal journal = new Journal(progressor);
Batch<MyGear> gear = crawler.crawl(journal, ReindexScope.all())
        .query(MyGear.class)
        .pull();
```

Once supplied, progress control belongs to the journal. Calling
`journal.cancel("Stopping.")` is thread-visible and aborts the crawl at its next
checkpoint. Successful operations complete their journal automatically;
failures and cancellation retain distinct terminal states.

Journals fail early by default. A catalogue-validation crawl can instead record
every independently recoverable clue-finding or gear-resolution exception and
fail after all recoverable work has been examined:

```java
Journal journal = new Journal(FailureMode.FAIL_LATE);
try {
    crawler.crawl(journal, ReindexScope.all());
} catch (CrawlException report) {
    List<Exception> allFailures = report.failures();
}
```

The final exception retains every occurrence in encounter order while its
message shows only the first 50. An archive with a clue-finding failure is not
stored or resolved, but clean archives continue into resolution. A resolution
failure is recorded against its artifact ARI; that artifact contributes no
gear, while its descendants and the remaining archives are still examined.
RetroCrawler throws the final report before constructing or parking the Stash,
so a failed operation never exposes a partial result.

---

## Design Goals

- No database
- No imposed interfaces on user domain models
- Strong typing without forcing predefined structures
- Incremental and cacheable processing
- Minimal core dependencies
- Suitable for both CLI tools and GUI applications (e.g. Vaadin)

---

## Prerequisites

- Electricity
- Java 21+
- Apache Maven
- Node v25 (Vaadin demo app only)

## Java cleanup and formatting

RetroCrawler uses the headless Java formatter from the separate
[Westarps devtools](https://github.com/jewesta/devtools) repository. It first
applies a conservative OpenRewrite cleanup, including import sorting and
unused-import removal, and then formats the result with Eclipse JDT. The
canonical `prettify/formatting-rules.xml` profile in devtools can also be
imported directly into Eclipse or STS.

By default, the launcher expects `devtools` beside `retro-crawler`. Override
the location with `WESTARPS_DEVTOOLS_HOME` or with repository-local Git
configuration:

```sh
git config --local westarps.devtools.path /path/to/devtools
```

Check all tracked Java sources without writing changes:

```sh
run/prettify.sh --assert
```

Clean up and format particular files:

```sh
run/prettify.sh --apply path/to/First.java path/to/Second.java
```

The matching `run/prettify.bat` launcher provides the same interface on
Windows. Maven preparation is enabled by default so OpenRewrite can resolve
types correctly; `--no-prepare` skips that build when the reactor outputs are
already current.

---

## Demo App

Since RetroCrawler is a library, we provide a demo app based on the Vaadin UI framework so you can see how all comes together. You can use this as a starting point for building your own gui. But please note that compared to `retro-crawler-core` keeping `retro-crawler-app` stable is not a priority. Anything might change any time.
![RetroCrawler Demo App](docs/images/retro_crawler_demo_app.png)
You can run the demo app via a provided shell script (macOS) or batch file (Windows). CD into `/run` located in the root of the repository. Then run the script. This should build and install RetroCrawler and launch the Vaadin app. Once it runs you can access it via `localhost:8080`.

The archive selector offers the Retro PC demo through both the local filesystem
folder and its adjacent ZIP file. Selecting an entry and pressing **Reindex**
demonstrates that both providers produce the same collection. Image previews
are read through the selected `ArchiveSource`; opening a local archive folder
is available only for the filesystem variant. Demo material is copied to the
working directory below `rc_demo_archives` when needed. RetroCrawler creates a
local `cache` directory for its JSON clue archive; both generated directories
are ignored by Git.

#### macOS
```sh
cd run
./app.sh
```

#### Windows
```batch
cd run
app.bat
```

## Command Line Demo

Even though the demo app is nice: If you do not want to install the black hole that is `node.js` and then watch maven download half the internet just to see how RetroCrawler works we have also created a command line demo. Just like the Demo App above you can run it via `cli.sh` or `cli.bat` respectively.

Just don't expect anything spectacular.

---

## Status

RetroCrawler is currently under active development.
APIs may evolve, but we try to keep core concepts stable.

Again: The apps (Vaadin / command line) are just there for demo purposes and so you have something to copy to get going.

## Legal

Developed following Starfleet Corps of Engineers (SCE) standard procedure. May only be used inside the juristiction of the United Federation of Planets.
