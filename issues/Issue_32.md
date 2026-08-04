# Issue 32: Arbitrary Archive Sources

## Intent

Separate archive crawling from the local filesystem so RetroCrawler can inspect
the same rooted, hierarchical archive model through other providers such as SSH
or an object-store adapter. RetroCrawler must continue to own planning,
filtering, depth-first traversal, clue boundaries, cancellation, and progress.
Providers supply direct folder listings and optional file-content access; they
do not perform the crawl themselves.

## Domain Model

- An `ArchiveSource` opens one `ArchiveSession` for a configured root path.
- An `ArchiveSession` exposes its root folder, classifies direct children in an
  `ArchiveListing`, and may provide file contents.
- `ArchiveFolder` and `ArchiveFile` are provider-owned, read-only handles. Their
  paths are source addresses and are not assumed to belong to the local
  filesystem.
- File content is inspected through a generic `ArchiveFileAccessor`. The
  session creates and always closes the stream; clue finders never own it.
- `Optional.empty()` from content access means that the provider deliberately
  cannot expose content for that file. An `IOException` remains an access
  failure.
- `FileSystemArchiveSource` is the default provider and preserves existing
  `Path`/`Files` behavior behind the new boundary.

The selected source is runtime crawler composition rather than collection
model vocabulary. Annotation and `ArchiveRoots` paths therefore remain useful
defaults, while `RetroCrawler.Builder` accepts an alternate source.

## Decisions

1. Keep `Path` as the configured and exposed hierarchical address for this
   change. Providers interpret it; core crawl code must not call `Files` on it.
   This preserves cache relocation, partial re-indexing, filters, relative file
   clues, path facts, and application source-path APIs.
2. Use one source instance for all configured roots of a crawler. Stateful
   providers may create one independent session per root.
3. Let the provider classify folder and regular-file entries once. Planning
   retains and reuses that listing exactly as it does today.
4. Make content access optional per file rather than introducing a capability
   hierarchy. Missing content contributes no content-derived clue.
5. Close sessions around complete planning/digging operations and close file
   streams within the synchronous accessor call, including exceptional exits.
6. Preserve `ArchivePathFilter` and `FileNameClueFinder` contracts for now.
   They receive source paths but cannot assume those paths are locally
   accessible.

## Completed Work

- [x] Added the archive source/session/entry/listing/accessor contracts.
- [x] Implemented and contract-tested `FileSystemArchiveSource`.
- [x] Refactored crawl planning and digging to use source sessions.
- [x] Routed path and tree clue content inspection through scoped accessors.
- [x] Added alternate-source composition to `RetroCrawler.Builder`.
- [x] Preserved complete and partial re-index behavior through source sessions.
- [x] Documented provider behavior and updated the README example.
- [x] Ran focused, reactor, and packaged verification.

As a follow-up provider, `ZipArchiveSource` crawls a local ZIP file directly
without extraction. The ZIP path is both the configured source location and
the logical root address. The provider indexes the central directory once per
session, creates folders implied by nested file entries, and uses the same
session-owned accessor contract for entry content. Malformed entry names that
escape the root, use non-portable separators, duplicate paths, or create
file/folder collisions are rejected when the session opens.

The crawl planner and digger now operate on provider handles and listings rather
than interpreting source addresses through `Files`. `ArchiveManager` owns the
session lifetime for both complete and partial re-indexing, including provider
listing walks used to resolve partial scopes. The filesystem implementation is
the default, so existing crawler construction continues to work unchanged.

Content access is synchronous and session-owned:

```java
<T> Optional<T> access(ArchiveFile file, ArchiveFileAccessor<T> accessor)
        throws IOException;
```

This keeps stream closure inside the provider boundary. Tests cover successful
and exceptional accessor returns, unavailable content, session closure, and a
memory source whose addresses do not exist on the local filesystem. The stored
archive representation remains compatible, so no cache format or version
change was required.

## Verification

- Canonical formatter applied to changed Java files and check-only assertion
  passed.
- Focused core verification passed: 32 tests covering the source contracts,
  source-driven digging, builder composition, and archive management.
- ZIP verification passed: six provider contract and malformed-archive tests,
  plus an end-to-end crawl test that reads a nested clue without extraction.
- The complete core suite passed with 180 tests.
- `mvn test` passed for the complete seven-module reactor.
- `mvn clean install` passed for the complete seven-module reactor and packaged
  module boundaries.
