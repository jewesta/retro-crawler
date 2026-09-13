# Repositories and operations

Repositories keep the extracted clue Archive so later resolution and queries
do not require another physical crawl. A `Journal` owns the lifecycle of each
access or crawl operation. See the [README](../README.md) for the project
overview.

## Archive repositories

Applications explicitly select a `Repository`. `JsonFileRepository` is the
bundled persistent implementation and uses the local `cache` directory when
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

For applications that need extracted Archives only for the lifetime of the
process, the framework supplies a thread-safe `InMemoryRepository`:

```java
Repository repository = new InMemoryRepository();
```

An in-memory repository writes no files and starts empty after an application
restart. A missing stored Archive causes its configured source to be crawled.
If retrieval fails, RetroCrawler reports the repository failure and rebuilds
the clue Archive from that source.

The JSON cache format is versioned. Its version is inspected before the stored
payload is deserialized, so an unsupported, missing, or malformed version is
rejected at the repository boundary rather than parsed as the current Archive
shape. Artifact-relative Clue sources are checked for traversal and cannot
escape the Artifact supplying their context.

Stored data is a rebuildable extraction of the physical Archive. It is never a
second source of truth.

## Journal and progress

Crawler operations accept an operation-scoped `Journal`. It owns the progress
lifecycle together with the failure record. The underlying `Progressor`
remains a neutral progress mechanism, while immutable structured progress is
available through `journal.progress()`.

A lightweight message view supports simple command-line and GUI integrations:

```java
Progressor progressor = Progressor.reportingMessages(System.out::println);
Journal journal = new Journal(progressor);
Batch<MyGear> gear = crawler.crawl(journal, ReindexScope.all())
        .query(MyGear.class)
        .pull();
```

Once supplied, progress control belongs to the Journal. Calling
`journal.cancel("Stopping.")` is visible across threads and aborts the crawl
at its next checkpoint. Successful operations complete their Journal
automatically. Failure and cancellation remain distinct terminal states.

## Failure handling

Journals fail early by default. A catalogue-validation crawl can instead
record every independently recoverable clue-finding or Gear-resolution
exception and fail after all recoverable work has been examined:

```java
Journal journal = new Journal(FailureMode.FAIL_LATE);
try {
    crawler.crawl(journal, ReindexScope.all());
} catch (CrawlException report) {
    List<Exception> allFailures = report.failures();
}
```

The final exception retains every occurrence in encounter order while its
message displays only the first 50.

An Archive with a clue-finding failure is neither stored nor resolved, but
clean Archives continue into resolution. A resolution failure is recorded
against its Artifact ARI; that Artifact contributes no Gear while descendants,
siblings, and remaining Archives continue to be examined.

RetroCrawler throws the final report before constructing or parking a Stash,
so a failed operation never exposes a partial collection result. Fatal
resolution failures remain in the Journal because no `GearNode` exists to
carry a `ResolutionTrace`.
