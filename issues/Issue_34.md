# Issue 34: One Crawler, Multiple Archives

## Intent

Let one `RetroCrawler` apply a shared `Model` to several independently
identified archives. Each archive owns its descriptor, roots, source provider,
repository identity, and crawl lifecycle while sharing clue interpretation,
fact parsing, gear matching, and model configuration.

The target composition is:

```text
RetroCrawler
├── Model: retro hardware
├── Archive: my_collection
│   └── filesystem roots
├── Archive: museum_collection
│   └── SSH roots
└── Archive: incoming_material
    └── ZIP roots
```

Several roots on one `ArchiveDescriptor` remain buckets of one logical
archive. Several descriptors registered with a crawler are separate logical
archives, with separate `ArchiveId` values and repository entries.

## Decisions

1. `Model` remains the shared interpretation model for all registered archives.
   For compatibility it still supplies the annotation-derived default archive.
2. `RetroCrawler.Builder.archive(...)` registers an explicit
   `ArchiveDescriptor` and `ArchiveSource`. Registering any explicit archive
   replaces the annotation-derived default archive for that crawler.
3. A crawler exposes all registered archive descriptors and archive-selected
   overloads of crawl and inspect operations.
4. Existing archive-unqualified methods remain convenient for a crawler with
   exactly one archive and reject ambiguous use on a multi-archive crawler.
5. Archive IDs must be unique within a crawler. Repository entries are already
   separated by `ArchiveId`.
6. Crawl planning and the repository remain crawler-wide composition for now.
   Source selection is archive-specific.
7. A logical `URI` resource address remains a promising follow-up. This change
   keeps `Path` source addresses and makes their archive context explicit with
   an `ArchiveId`.

## Compatibility

Existing construction remains valid:

```java
RetroCrawler crawler = RetroCrawler.builder()
        .model(model)
        .repository(repository)
        .build();
```

It creates one crawler archive from `model.archiveDescriptor()` and uses the
filesystem source unless `archiveSource(...)` overrides it.

## Progress

- [x] Add multi-archive crawler composition and archive-selected operations.
- [x] Preserve and test the single-archive compatibility API.
- [x] Demonstrate one shared model with filesystem and ZIP archives.
- [x] Document the public composition model.
- [x] Run focused and reactor verification.

## Verification

- Canonical `prettify` assertion passed for all changed Java sources.
- `mvn -pl retro-crawler-app -am test` passed.
- `mvn clean install` passed for the full seven-module reactor.
