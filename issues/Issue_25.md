# Issue 25: External, Configurable Fact Catalogs

## Context

RetroCrawler must keep substantial third-party catalog data out of its public
repository unless that data is explicitly licensed for redistribution. The
personal collection module may eventually move to a private repository, but
the framework should first make catalog data external, user-editable, and
replaceable without changing parser code.

The existing Nintendo Game Boy cartridge catalog is permissively licensed and
can remain bundled. It provides the first real migration target for the generic
catalog mechanism.

## Intent

- Define one strict, lightweight TSV catalog format in the core.
- Load a catalog only when a fact parser that uses it is actually selected by a
  discovered `@RetroFact`.
- Give catalog-backed fact parsers a standard implementation and immutable
  catalog access.
- Let a collection override parser defaults through annotations or runtime
  model configuration.
- Keep external catalogs and other crawler-owned files below a configurable
  collection working directory.
- Separate collection identity and locations from clue-finder configuration.

## Agreed Public Vocabulary

```java
@RetroCollection(
    id = "my_collection",
    name = "My Collection",
    locations = { "..." },
    workingDirectory = "..."
)
@RetroClues(
    fromFolderName = BracketClueFinder.class,
    fromFileNames = { StandardImageClueFinder.class },
    fromFileContents = { RetroMarkdownClueFinder.class }
)
@RetroFactParser(
    parser = ManufacturerFactParser.class,
    catalogFile = "manufacturers.tsv"
)
public final class MyCollection {
}
```

- `@RetroCollection` replaces `@RetroArchive` and is present on exactly one
  type per `Model`.
- `@RetroClues` owns all crawl-time clue-finder configuration.
- `PathNameClueFinder` becomes `FolderNameClueFinder`, because it receives only
  the current folder name.
- `@RetroFactParser` is repeatable and configures a parser for the collection.
  It does not select or instantiate that parser.
- Field-level `@RetroFact(parser = ...)` remains the parser-selection point.

## Catalog Format

The generic core type is `Catalog<K extends Enum<K>>`.

- UTF-8 tab-separated text.
- Any number of `#` comment lines and blank lines.
- The first non-comment line is the header.
- Header cells are exactly the enum constant names.
- Header order is arbitrary.
- Every enum key must appear exactly once.
- Missing, unknown, and duplicate headers are rejected.
- Every data row must have exactly the header's number of cells.
- Trailing empty cells are retained.
- Catalog rows retain raw strings; domain parsers perform typed interpretation.

## Parser Lifecycle

`CatalogFactParser<K>` extends the normal `FactParser` SPI and exposes its
`Catalog<K>`. `AbstractCatalogFactParser<K>` loads that catalog through a
parser-scoped `CatalogLoader` and supplies the getter.

The framework continues to own parser construction. A catalog parser used by a
discovered fact is constructed with a `CatalogLoader`. A parser merely present
on the classpath, in an annotation, or in builder configuration is not
instantiated and causes no catalog access.

Source precedence is:

1. Runtime `Model.Builder` parser configuration.
2. Collection-level `@RetroFactParser` configuration.
3. The parser's declared default catalog.

A configured catalog file is relative to `<workingDirectory>/catalogs`.
Framework parsers may bundle their default catalog resource. If no bundled
default exists, the loader can use the parser's default filename below the
catalog directory. Missing required configuration fails during model creation
once the corresponding parser is actually used.

## Model Construction

`Model.Builder` will own inputs needed before parser construction:

- discovered or explicitly supplied model types;
- runtime collection locations;
- runtime working directory;
- runtime fact-parser configuration.

Annotation values are portable defaults. Builder values override them. The
existing `RetroCrawler.Builder` continues to compose a completed model with a
repository and crawl-planning behavior.

The working directory is collection-wide and may eventually contain:

```text
<workingDirectory>/
  catalogs/
  cache/
  reports/
```

Supplying it must not eagerly create, scan, or access those directories.

## Clue/Fact Lifecycle

- Changing `@RetroClues` changes observed evidence and requires recrawling.
- Changing a fact parser or its catalog reinterprets the existing clue archive
  and does not require recrawling.
- Catalog rows and resolved facts never enter the persisted clue archive.

## Progress

- [x] Design agreed.
- [x] Collection and clue annotation migration.
- [x] Generic catalog infrastructure.
- [x] Catalog-backed parser construction.
- [x] Model builder and override precedence.
- [x] Game Boy catalog migration.
- [x] Tests and documentation.

## Implemented State

The public API now contains:

- `@RetroCollection`, `@RetroClues`, and repeatable `@RetroFactParser`.
- `FolderNameClueFinder` and `BlindFolderNameClueFinder`.
- `Catalog<K>`, its immutable key-addressed rows, and strict TSV validation.
- `CatalogLoader`, `CatalogFactParser<K>`, and
  `AbstractCatalogFactParser<K>`.
- `FactParserConfiguration` and `Model.Builder` runtime overrides.

`GearResolverFactory` recognizes catalog parsers and constructs them through a
public `CatalogLoader` constructor. Plain fact parsers retain their public
no-argument construction contract. Configuring `catalogFile` on a used plain
parser is rejected explicitly.

The Game Boy parser now extends `AbstractCatalogFactParser` and its typed lookup
view is built from a generic enum-keyed `Catalog`. Direct no-argument parser use
continues to load its licensed bundled snapshot, while framework construction
uses the standard loader and can replace it with an external file.

The demo and collection adapter use the new collection/clue annotations. The
personal bracket parser is now `BracketClueFinder`; no private paths or catalog
data were added.

## Verification

- `mvn -pl retro-crawler-core test`: 124 tests passed.
- `mvn -pl retro-crawler-model -am test`: core and shared-model tests passed.
- `mvn test`: all eight reactor modules passed after the API migration.
- `mvn clean install`: all eight modules compiled, tested, packaged, and
  installed successfully from a clean build.

## Manufacturer Catalog Follow-up

The first private catalog candidate is a manufacturer directory. Its standard
model keeps the manufacturer identity readable while retaining a typed external
reference needed by consumers:

- `Manufacturer` contains a usual name, an optional full corporate name, and an
  optional `TheRetroWebReference` restricted to the manufacturer category.
- The strict catalog schema contains `name`, `full_name`, and `trw_id`; the ID
  cell may be empty. This is a typed schema field rather than arbitrary custom
  metadata.
- A resolved manufacturer therefore retains enough information for a UI to
  render the numeric TRW deep link through `TheRetroWebReference.lookupUri()`.
- Logo paths and source URLs remain in private acquisition provenance. Unlike
  the stable TRW identifier, they describe local files or acquisition context.
- `ManufacturerParser` matches short and full names exactly while ignoring case
  and surrounding whitespace. If one observed name maps to several catalog
  entries, resolution remains explicitly ambiguous instead of choosing a row.
- The bundled catalog contains one independently sourced ASUS identity. The
  substantial TRW-derived snapshot remains a local override and is not added to
  the repository.

The private snapshot was transformed into the strict schema with its TRW IDs
but without its logo column. All 2,913 rows load successfully; ASUS resolves
uniquely with a linkable reference, while the two distinct `Umax` rows preserve
their shared short-name ambiguity.

Follow-up verification:

- `mvn -pl retro-crawler-model -am test`: 124 core and 61 model tests passed.
- `mvn test`: all eight reactor modules passed.
- `mvn clean install`: all eight reactor modules packaged and installed successfully.
