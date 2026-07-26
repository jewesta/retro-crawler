# Issue 17: Pluggable Repository

## Original Intent

RetroCrawler currently persists the extracted clue archive as JSON files in an
automatically created cache folder. This is sufficient as a default, but the
hardcoded storage mechanism is not suitable as a framework extension point.

The original issue proposed:

- Creating a repository interface.
- Moving the current JSON-file behavior into an implementation of that
  interface.
- Considering additional convenient implementations, such as H2.

## Domain Model

The configured filesystem archive is the source of truth. Crawling it produces
an extracted `Archive` containing the clues used to create `Gear`.

`ArchiveManager` coordinates three possible sources:

1. Its in-memory archive.
2. An archive retrieved from a `Repository`.
3. A newly crawled archive, which is subsequently stowed away in the
   `Repository`.

The repository only stores and retrieves extracted archives. It does not decide
when crawling or rebuilding should happen; that policy belongs to
`ArchiveManager`.

## Language and Naming

The name `Repository` is intentionally concise because it already lives in the
archive context. Prefixing every related type with `Archive` would make the API
needlessly repetitive.

The operations `stowaway` and `retrieve` are also intentional. RetroCrawler uses
playful language based on real-world archive behavior rather than conventional
CRUD terminology such as `save` and `load`.

The intended minimal contract is:

```java
public interface Repository {

	void stowaway(Archive archive);

	Optional<Archive> retrieve(ArchiveId id);
}
```

## Decisions

- Keep the names `Repository`, `stowaway`, and `retrieve`.
- Keep JSON files as the backward-compatible default implementation.
- Make the repository an injectable framework extension point.
- Keep repository implementations independent of crawling and rebuild policy.
- Do not add an H2 implementation as part of this issue.
- Do not add H2 or another database dependency to `retro-crawler-core`.
- Treat a failed retrieval as a reason for `ArchiveManager` to crawl the
  filesystem source again.
- Keep failures while stowing away visible to the caller.
- Use value semantics for archive IDs so custom repositories can safely use
  them as map keys.

## Public API Goal

RetroCrawler is intended to be a toolset rather than a closed application.
Consumers should be able to assemble a crawler through an elegant builder and
replace individual tools through clear public interfaces.

The builder should be the discoverable composition surface for application
developers. It should:

- Require the annotated types from which a crawler is built.
- Supply useful defaults, including `JsonFileRepository`.
- Accept an explicitly configured `Repository`.
- Keep storage-specific conveniences out of the general crawler API.
- Leave runtime concerns, such as the `Monitor` and reindexing, on crawl
  operations rather than construction.

Public extension-point interfaces should describe domain capabilities.
Implementation and orchestration classes should not become public configuration
surfaces merely because the builder uses them internally.

## Starting Point

Commit `20b2851` introduced:

- `Repository`
- `RepositoryException`
- `JsonFileRepository`
- Delegation from `ArchiveManager` to the repository interface

At that point the abstraction was not complete: `ArchiveManager` still
constructed `JsonFileRepository` directly and there were no tests covering
repository behavior.

## Current Implementation Direction

The public builder should act as the composition point. It can delegate the
reflection-based assembly to `RetroCrawlerFactory`:

```text
RetroCrawler builder
        |
        v
RetroCrawlerFactory
        |
        v
RetroCrawlerImpl
        |
        v
ArchiveManager
        |
        v
Repository
```

Omitting repository configuration from the builder should preserve the existing
JSON cache behavior. An explicitly supplied repository is passed through
`RetroCrawlerFactory` and `RetroCrawlerImpl` to `ArchiveManager`.

Issue 17 supplies the repository contract, JSON implementation, and injection
seam. The unified builder and its `.repository(...)` entry point belong to issue
21. Until that builder is implemented, `RetroCrawlerFactory` accepts the
repository directly.

`JsonFileRepository` retains its no-argument default and also accepts a
configurable cache directory.

## Implemented Behavior

- `Repository` documents the storage contract for extracted clue archives.
- `JsonFileRepository` remains the default.
- `JsonFileRepository(Path)` selects a custom directory.
- Repository selection is passed from `RetroCrawlerFactory` through
  `RetroCrawlerImpl` to `ArchiveManager`.
- Retrieving a missing archive has no filesystem side effects.
- Retrieval failures are reported and cause the filesystem source to be
  crawled again.
- Failures while stowing away remain visible to the caller.
- JSON writes use a temporary file followed by an atomic replacement where the
  filesystem supports it.
- Archive IDs are safely encoded for use in filenames.
- Retrieved JSON is rejected if its archive ID does not match the requested ID.
- IDs derived from `AbstractId` have value-based equality and hash codes.

## Implementation Checklist

- [x] Extract the `Repository` interface.
- [x] Extract the current behavior into `JsonFileRepository`.
- [x] Introduce `RepositoryException`.
- [x] Inject `Repository` through `RetroCrawlerFactory`, `RetroCrawlerImpl`, and
      `ArchiveManager`.
- [x] Make the JSON repository directory configurable while preserving `cache`
      as the default.
- [x] Remove extraction leftovers such as unused imports and logging fields.
- [x] Add focused tests for repository retrieval, stowing away, cache misses,
      reindexing, and custom repository injection.
- [x] Update user-facing documentation to describe the default and the extension
      point.

## Deferred Possibilities

- What should the exact builder entry point look like, and should
  `RetroCrawlerFactory` remain part of the primary public API or become an
  implementation detail behind the builder?
- Is a no-op or in-memory repository useful enough to ship, or should such
  implementations remain test fixtures until a concrete use case appears?

## Verification

- `JsonFileRepository` round trip, replacement, filename safety, invalid JSON,
  and archive-ID validation are covered by focused tests.
- `ArchiveManager` repository hit, miss, reindex, retrieval failure, and
  in-memory reuse behavior are covered by focused tests.
- Custom repository propagation through `RetroCrawlerFactory` is covered by a
  focused test.
- The complete Maven reactor passes `mvn clean install`.

## Out of Scope

- H2 or JDBC persistence.
- A normalized relational schema for clues and archive nodes.
- Changing the filesystem archive as RetroCrawler's source of truth.
