# Modeling and querying

RetroCrawler discovers plain Java collection and Gear types, interprets Clues
as typed Facts, and exposes the resolved Gear as an immutable queryable Stash.
The [README](../README.md) provides the shorter project overview.

## Annotation configuration

A collection model is normally described with these annotations:

- `@RetroCollection` declares the collection identity, optional working
  directory, and `ArchivePathFilter`s. Exactly one collection is present in a
  model. Archive roots and providers remain runtime crawler configuration.
- `@RetroClues` declares the ordered `ClueFinder`s that inspect each
  candidate's pruned archive view. Each finder must be a named class with a
  simple name unique within the configured list because that compact name is
  retained as durable provenance.
- `@RetroGear` declares a Gear type and its matcher.
- `@RetroAnyGear` declares the optional fallback Gear type used when no matcher
  recognizes an Artifact or equally confident matches cannot be resolved
  through their type hierarchy. A model may have at most one fallback.
- `@RetroFact` declares how a Gear field is populated from a Clue.
- `@RetroFactCatalog` optionally overrides the catalog used by a
  catalog-backed parser. It does not select or instantiate that parser.
- `@RetroAnyAttribute` captures remaining unassigned Facts and is especially
  useful on a fallback Gear type.

These declarations keep the framework strongly typed without requiring Gear
classes to implement framework interfaces.

## Source filtering

Archive noise can be pruned before entries are supplied to Clue finders. The
bundled opt-in filters cover dot-prefixed entries and common operating-system
or NAS service paths:

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
no-argument constructor for annotation use or inject configured instances
through `Model.Builder.pathFilters(...)`.

Rejecting a folder prunes its complete subtree during planning and digging.
Changing filters requires a reindex because a retrieved clue archive retains
the result of the crawl that produced it.

## Model discovery

Annotated collection and Gear types are discovered recursively below an
application package:

```java
Model model = Model.from("com.example.collection");
```

Applications that need a deterministic or custom discovery boundary can
provide a `Set<Class<?>>` or a `TypeSource` instead.

A model declares how the collection is interpreted, never where it is stored.
Deployment-specific settings may override annotation defaults while the model
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
when a discovered field-level `@RetroFact` selects that parser.

Catalogs use a strict UTF-8 TSV format. Blank and `#` comment lines are
allowed. The first data line contains enum constant names as headers; every
key must occur exactly once, while column order is arbitrary.

## Matching and construction

A `GearMatcher` sees the complete detection Fact set for one Artifact and
returns a confidence. When equally confident matching Gear types form one
inheritance chain, RetroCrawler selects the most specific type. An unrelated
tie is ambiguous and falls back to `@RetroAnyGear` when the model declares
one.

The selected `GearFactory` creates the object and field-level parsers produce
the final attributes declared by its `@RetroFact` annotations. Detection and
final attributes remain separate because some Facts become applicable only
after a Gear type has been selected.

## Typed Stash queries

A Stash contains all recognized Gear in its natural archive hierarchy. A typed
immutable query is materialized as a lifted `Batch<G>`:

```java
Batch<RetroHardware> working = stash.query(RetroHardware.class)
        .where(museumCollection.id())
        .where(RetroHardware::isWorking)
        .pull();

Batch<RetroHardware> agpCardsAmongEverythingElse =
        stash.query(RetroHardware.class)
                .whereIf(GraphicsCard.class,
                        card -> card.bus() == ExpansionBus.AGP)
                .pull();
```

`whereIf` applies its predicate only to the named Gear type and retains other
selected Gear. A Batch keeps its archive groups through `archives()` and
exposes their cumulative forest through `roots()`. Its flat `gear()` view is
derived lazily and cached.

Every `GearNode` retains the ARI of the Artifact that produced it. Complete
Stash construction validates Retro ID uniqueness across every registered
Archive. A subtree crawl is routed by its ARI while other Archives reuse their
stored clue archives.

## Resolution traces

Every Gear node produced by RetroCrawler carries a focused
`ResolutionTrace`. It retains the source Artifact, detection and final
attribute snapshots, every matcher decision, the selected match, and non-fatal
ambiguities:

```java
GearNode<RetroHardware> node = working.roots().getFirst();
ResolutionTrace trace = node.trace().orElseThrow();

List<Fact> factsUsedToBuildGear = trace.resolved().facts();
List<ResolutionTrace.Match> consideredTypes = trace.matches();
```

Queries preserve traces while lifting nodes into a Batch. Programmatically
constructed `GearNode`s may have no trace. Traces are resolved Stash state;
they are neither written to the clue cache nor injected into user Gear
objects. Fatal resolution failures that produce no node remain in the
operation `Journal`.

## Structured filters

Every semantic Fact key contributes exactly one `FilterDefinition`, even when
several Gear types declare that Fact. The definition records its value type,
cardinality, applicable Gear types, and the `FilterType` supplied by the
effective parser. Built-in string parsers describe text filters, enum parsers
describe ordered choices, integer and temporal parsers describe ranges, and a
custom parser defaults to exact equality.

`crawler.filters()` exposes the complete model vocabulary. A Stash or Batch
exposes the same definitions and lazily computes their observed availability:

```java
FilterAvailability.Choices<?> available =
        (FilterAvailability.Choices<?>) stash.availability(busFilter);

List<? extends FilterAvailability.Option<?>> options = available.options();
Batch<RetroHardware> agp = stash.query(RetroHardware.class)
        .where(busFilter, ExpansionBus.AGP)
        .pull();
```

`filters(FilterSelection.ALL)` returns the complete model vocabulary.
`filters(FilterSelection.RELEVANT)` keeps definitions with at least one
populated occurrence in that Stash or Batch. Zero-argument `filters()` remains
the shorthand for all definitions.

A choice remains in `options()` even when absent; its
`matchingOccurrences()` is then zero and `present()` is false. Counts describe
Gear occurrences rather than repeated raw values. A criterion applies to Gear
types declaring its Fact, rejects applicable Gear without a matching value,
and retains Gear types to which the Fact does not apply. Batch availability is
recomputed over the materialized result so a UI can update its facets without
losing globally conceivable choices.

## Optional shared model

RetroCrawler remains a bring-your-own-type framework. The optional
`retro-crawler-model` module supplies portable value types and canonical
parsers for meanings stable across collections, including ISBNs, MAC
addresses, category-qualified The Retro Web references, expansion buses,
memory vocabulary, measurements, and data capacities.

Collection adapters remain responsible for mapping their folder tags,
metadata files, and other Clues onto that shared vocabulary. The core does not
depend on the shared model.
