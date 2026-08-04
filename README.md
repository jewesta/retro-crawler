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
Aggregation is a crawler operation: `crawlAll` resolves every archive in one
pass and validates Retro ID uniqueness across all of them.

### Artifact
An optional representation of a single folder in the archive.
An artifact exists only if meaningful information can be extracted from that folder which we call `Clue`s. This is a raw representation of a single **potential** piece of your collection.

### Clue
A raw key–value observation derived from:
- folder names
- file names
- file contents

Clues are always **string-based** and may contain multiple values. This is a raw representation of a **potential** property of a piece in your collection.

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

Most clue finders inspect only the current folder name or its direct files.
Collections with meaningful metadata subtrees may additionally configure
`TreeClueFinder`s. These run depth-first in post-order through a transient
`ArchiveFolderView`, may inspect file content lazily through
`ArchiveFileView.peek(...)` when the configured source exposes it, and return
clues for the current folder. Child
folders that already established an artifact are pruned from the view, so a
finder cannot cross into another potential collection part.

### 2. Gear / Fact Phase
All known clues are converted into facts using registered parsers.
Based on these facts, `GearMatcher`s determine which gear type best represents an artifact.
The corresponding `GearFactory` then creates the final domain object.

Unknown or unparseable clues are preserved and may be accessed explicitly.

---

## Configuration via Annotations

RetroCrawler's collection and gear model can be configured via annotations:

- `@RetroCollection`
  Declares the collection identity, source locations, optional working
  directory, and `ArchivePathFilter`. Exactly one collection is present in a
  model.

- `@RetroClues`
  Declares which folder names, file names, file contents, and folder trees
  produce crawl-time clues.

- `@RetroGear`
  Declares a gear type and its matcher.

- `@RetroFact`
  Declares how a field is populated from a clue.

- `@RetroFactCatalog`
  Optionally overrides the catalog used by a catalog-backed fact parser. It
  does not select or instantiate that parser.

- `@RetroAnyAttribute`
  Captures all remaining unassigned facts. Especially useful on "catch all" default gear types that are produced if none others match.

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
file. The ZIP path is the logical archive root. Entry names become descendant source
paths, and folders omitted from the ZIP directory are inferred from their
children.

A session supplies its root folder and classified direct listings while
RetroCrawler retains control of planning and depth-first traversal. File
content is optional. When available, the session invokes a generic
`ArchiveFileAccessor` synchronously and closes the supplied `InputStream`
before returning its result. An empty result means that content was not
available and content-based clue finders contribute no clue for that file.

Applications can inspect a file at any source path produced by the crawler
through the same scoped accessor contract. Every address is qualified by the
archive it belongs to:

```java
Optional<byte[]> image = crawler.inspect(
        incomingMaterial.id(), sourcePath, InputStream::readAllBytes);
```

The crawler resolves the address through its configured source and closes the
short-lived session as well as the content stream. `Optional.empty()` means the
provider recognizes the file but does not expose its content; a missing or
folder address raises `NoSuchFileException`.

Source paths remain hierarchical addresses used for relative clues, cache
relocation, and partial re-indexing; providers must not require them to be
locally accessible.

Crawling either selects one archive by identity or spans all of them:

```java
Stash<RetroHardware> museum = crawler.crawlStash(
        museumCollection.id(), progressor, reindexScope, RetroHardware.class);

Stash<RetroHardware> everything = crawler.crawlAllStash(
        progressor, reindexScope, RetroHardware.class);
```

A `Stash` keeps its gear grouped per archive, so every result retains the
archive it came from. `crawlAll` validates Retro ID uniqueness across all
registered archives and routes a subtree reindex scope to the archive whose
root contains each requested path; archives without a requested subtree reuse
their stored clue archive.

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

---

## Progress and Cancellation

Crawler operations accept a `Progressor`. It publishes immutable, structured
snapshots with an extensible stage, human-readable message, exact or
approximate work units, timing, and operation state. A lightweight message
view is available for simple command-line or GUI integrations:

```java
Progressor progressor = Progressor.reportingMessages(System.out::println);
List<MyGear> gear = crawler.crawlAllGear(progressor, ReindexScope.all(), MyGear.class);
```

Calling `progressor.cancel("Stopping.")` is thread-visible and aborts the crawl
at its next checkpoint. For larger workflows, progressors can be divided into
nested equal or weighted windows with `splitIntoEqualParts(...)` and
`splitInRelationTo(...)`.

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
