# Issue 22: Add a Structured Query API for Human and AI Collection Exploration

## Context

RetroCrawler was originally expected to culminate primarily in a Vaadin
application. Capable AI agents make a broader product boundary more useful:
RetroCrawler can become the deterministic collection-intelligence toolkit
through which people, applications, and agents explore a collector's Gear.

This does not make the Vaadin sample application obsolete. It remains valuable
as a visual browser, demonstration, and integration check. It should consume
the same public collection API as the CLI and future protocol adapters rather
than define the architecture itself.

## Intent

Provide a structured, discoverable API through which users can access and
explore all Gear recognized across a configured collection.

RetroCrawler should perform the deterministic work:

- Crawl filesystem archives or retrieve their cached clue archives.
- Extract model-independent clues.
- Resolve clues into typed facts and Gear.
- Preserve the evidence and provenance behind each result.
- Expose the complete resolved collection through a user-facing Stash.
- Answer explicit structured questions about that Stash.

An AI agent may translate a person's natural-language question into structured
queries. Natural-language interpretation itself does not belong in
`retro-crawler-core`.

> RetroCrawler turns collectors' filesystem archives into a Stash of
> structured, traceable Gear that humans, applications, and AI agents can
> explore.

## Reconciled Concept (2026-08-18)

This section reconciles the original Issue 22 query direction with the later
RetroCrawler concept discussion. It replaces earlier provisional designs based
on `GearEntry`, a public `StashSnapshot`, a `FindingAid` facade, or a
`Hierarchy` wrapper. The `Batch<G>` decision was revisited once Batch gained
responsibility for a typed lifted hierarchy rather than merely wrapping a list.

The settled vocabulary is:

- `RetroCrawler` is the configured crawling and resolution engine.
- `Stash` is the long-lived, user-facing interface to all currently recognized
  Gear.
- `Stash` remains the non-generic “everything” object.
- `stash.query(Type.class)` creates an immutable typed `Query<G>`; criteria
  narrow that query without changing the Stash.
- `Query.pull()` materializes an immutable typed `Batch<G>` whose hierarchy
  lifts matching descendants through excluded Gear. Batch retains archive
  groups, also exposes their cumulative roots, and lazily caches its flat view.
- The source hierarchy is an immutable forest of `GearNode<Object>`.
- Every hierarchy node has an authoritative source ARI.
- A Gear object may optionally receive that ARI through `@RetroSource`.

## Unified Clue Discovery (2026-08-19)

The original clue-finder categories preserved an abandoned write-back model:
folder-name clues and immediate-file-name clues were distinct because a changed
clue was once expected to be written back to the corresponding source. With
clues now immutable crawl evidence and no write-back path planned, that
strictness no longer expresses a useful domain boundary.

Clue discovery therefore has one extension point:

```java
public interface ClueFinder {

    Clues find(ArchiveFolderView folder);
}
```

Every configured finder runs once for each candidate folder, in configuration
order, after the folder's children have been classified. The bottom-up crawl
avoids a chicken-and-egg dependency: children establish their own artifact
boundaries first, then the parent receives a structurally pruned view containing
only:

- the candidate folder's own name;
- its direct files;
- child folders positively established as clue-free metadata folders.

Child artifacts and failed folders carry no readable view upward. A finder may
walk the remaining metadata subtree but cannot cross into another potential
piece of Gear. Folder ancestry remains location, never type evidence.

Finders decide for themselves which parts of the view they inspect. Several
finders may inspect the same file and one finder may inspect several files; the
old one-finder/one-file restriction had meaning only for write-back. Independent
finders still may not claim the same explicit clue key, so source ownership is
enforced at the evidence level rather than by assigning files to finder types.

No abstract finder hierarchy is introduced. Helpers may be added later when
concrete implementations reveal genuine reusable mechanics, but inheritance is
not part of the public contract.

This is deliberately a breaking refactor. `FolderNameClueFinder`,
`FileNameClueFinder`, `FileContentClueFinder`, `TreeClueFinder`, the blind
sentinel, and the `ArchiveFolderClueFinder` dispatcher are removed rather than
retained as compatibility facades. `@RetroClues` now declares one ordered array
of `ClueFinder` classes.

Diagnostics remain precise without specialized finder types or inferred
access provenance. Every folder and file view has an authoritative ARI. A
failure raised inside `ArchiveFileView.peek(...)` is attached to that file ARI;
other finder failures accurately default to the candidate folder ARI.

The completed refactor spans core, demo, personal collection adapter, app, and
CLI. Focused tests cover post-order pruning, independent finder ordering,
duplicate-key rejection, fail-late continuation, several finders inspecting the
same file, exact file-failure ARIs, zero/one/multiple explicitly declared
sources, finder-name collisions, synthetic clue provenance, and JSON
round-tripping. Canonical formatter assertions pass for all 35 added or modified
Java sources, and the full seven-module `mvn clean install` passes.

### Settled clue provenance

Finder identity and exact source provenance now live on `Clue` and survive the
`Artifact` cache boundary. They are distinct statements:

- The digger automatically attaches `finder.getClass().getSimpleName()` to
  every clue returned by that finder. Finder simple names form a model-local
  namespace. `ArchiveDigger` rejects anonymous, local, synthetic, or lambda
  finders, the same finder class configured twice, and distinct classes sharing
  one simple name. The validation error uses fully qualified names to explain a
  collision without bloating every serialized clue.
- A finder may explicitly declare zero, one, or several exact source ARIs per
  clue. An empty list is a deliberate absence of a resource-level claim.
  Accessing, listing, or peeking at a resource is never treated as evidence that
  the resource caused every clue returned by the finder.
- `ArchiveDefinition` supplies the collection namespace and derives stable ARIs
  through `ariFrom(resourcePath)`. `ArchiveFolderView` and `ArchiveFileView`
  receive those authoritative identities and offer `clue(...)` convenience
  methods that create a clue sourced from the view. Aggregate finders add the
  contributing ARIs deliberately while looping over their evidence; there is no
  bulk operation that assigns provenance to a completed `Clues` bundle.
- Framework-generated `@id` and `@folder` clues have neither a finder nor source
  list. Their origin is already inherent in their reserved keys.

The provisional `FinderObservation`, observed-view wrappers, `ClueSource`,
`ClueSourceKind`, and `ClueSighting` are removed. Crawl-time `ClueLocation`
offsets remain transient and are dropped at `Artifact`; finder names and source
ARIs are durable and are preserved by facts through their source clue.

Artifact JSON now stores each clue as a value/provenance object. `sources` is
omitted when empty, and the incompatible cache shape advances the archive cache
format to version 6. Compatibility with earlier issue-branch cache files is not
required.

The in-memory and public clue representation always retains complete ARIs. The
version 6 JSON cache avoids repeating their common prefixes by storing the
collection namespace once on the archive and using its existing tree position
as the artifact context. A source equal to the artifact is stored as `.`, and a
source below it as `./...`. Retrieval validates contextual paths and
reconstructs complete ARIs before constructing any clue. Relative paths
containing traversal are rejected at the repository boundary.

Source validity is stricter than path containment. Before accumulating a
finder's returned `Clues`, the digger checks every declared ARI against the
actual pruned `ArchiveFolderView` supplied to that finder. The candidate folder,
its files, and readable metadata descendants are valid; a child artifact or
failed/pruned folder is not valid even though its path is below the candidate.
One invalid source rejects the complete finder result atomically. Since no
valid source can lie outside this boundary, the cache accepts only contextual
`.` and `./...` source forms and has no full-ARI fallback.

The contextual cache follow-up is formatter-clean across its ten Java sources.
Its focused tests cover compact round-tripping, exact pruned-view enforcement,
collection identity, out-of-artifact rejection, and traversal rejection; the
full seven-module build passes.

### Product boundary

```text
archive sources          Repository
      |              (cached raw clues)
      '----------+-----------'
                 |
          crawl and resolve
                 |
         complete candidate state
                 |
          validate and install
                 |
                 v
              Stash
        /        |         \
   typed query  pull batch  structured query/trace
    Query<G>    Batch<G>       documents
        |          |             |
     Java/CLI    Vaadin       CLI/agents
```

`RetroCrawler` remains responsible for configuration, source access,
repository use, crawling, and resolution. `Stash` is the object users normally
talk to after configuration:

```java
RetroCrawler crawler = RetroCrawler.builder()
        // model, repository, archives
        .build();

Stash stash = crawler.access(journal);
```

`access` returns the parked Stash, resolves stored clues on cold access, and
crawls only where stored clues are missing. `crawl` deliberately rereads a
physical scope and returns a new complete Stash. The Stash is the communication
object between a user and their Gear, while RetroCrawler supplies its engine.

### Complete eager resolution

A Stash always represents all Gear recognized by the configured model. Gear
finding is not deferred until a type-specific query:

```text
archive sources
    |
    v
model-independent clue archives
    |
    v resolve every Artifact once
complete Stash state
    |
    v select and project in memory
typed batches, source hierarchy, queries
```

This gives the Stash truthful whole-collection semantics:

- Matching and construction failures occur while accessing or crawling the
  Stash rather than during an unrelated later pull.
- Retro ID uniqueness is validated across the complete configured collection.
- Every Artifact is parsed and matched once per produced Stash, not once per
  Gear type.
- Whole-collection statistics actually describe everything currently known.
- A batch is a predictable in-memory selection.

Expensive resource access remains deferred. Images, manuals, disk images, and
other file content are still inspected through their ARIs only when requested.

The resolved Stash is not persisted as another source of truth. The
`Repository` continues to store only rebuildable, model-independent clue
archives so model changes can reinterpret them without reindexing.

### Stash state and lifecycle

Each Stash is an immutable snapshot. The configured RetroCrawler parks the
current Stash reference; an explicit crawl builds and validates a complete new
candidate before atomically replacing that reference:

```text
current state -> build candidate -> validate -> install candidate
                       |
                       '-> failure: retain current state
```

Stashes, Queries, Batches, and hierarchy roots already returned to a caller
remain tied to the old snapshot; a later crawl never changes them. A public
`StashSnapshot` wrapper would therefore duplicate the Stash itself.

Lifecycle vocabulary is now:

- **access** — return the parked Stash; on cold access resolve stored clue
  archives, physically crawling only archives with no usable stored clues;
- **crawl** — explicitly reread the requested physical scope, resolve a new
  complete Stash, and park it only after success;
- **query/pull** — pure in-memory reads of one immutable Stash.

Each cold access or crawl is accompanied by one operation-scoped `Journal`. The
journal is the authority for progress stages, advancement, cancellation, and
terminal state as well as recoverable failures. The neutral `Progressor`
remains an injected mechanism behind it; operation code and callers report and
cancel through the journal, while observation is exposed read-only.

There is no public `resolve` operation. Repeated resolution through the same
immutable Model has no new input. After a JVM restart, cold `access` resolves
the persisted clues once; within the process it returns the parked Stash.

## Pulling a Batch

The Stash is deliberately non-generic and always means everything recognized
in the snapshot. It exposes the all-Gear shorthand and starts typed immutable
queries:

```java
Batch<Object> pull();

<G> Query<G> query(Class<G> desiredReturnType);
```

`query` uses assignability, so the desired type can be a concrete Gear class, a
shared application interface or base class, or `Object` when the model has no
more useful common denominator. Query criteria are immutable and composable:

```java
Batch<GraphicsCard> workingCards = stash
        .query(GraphicsCard.class)
        .where(archiveIds)
        .where(GraphicsCard::isWorking)
        .pull();

Batch<RetroHardware> agpCardsAmongEverythingElse = stash
        .query(RetroHardware.class)
        .whereIf(GraphicsCard.class, card -> card.bus() == Bus.AGP)
        .pull();
```

Archive and predicate criteria merely accumulate a selection recipe over the
same Stash; they do not copy, crawl, resolve, or mutate Gear. `pull` evaluates
all criteria once and constructs a `Batch<G>`. Its archive groups contain a
typed `GearNode<G>` forest. A matching descendant whose parent is excluded is
lifted to the nearest retained ancestor, or to an archive root if none remains.
The Batch retains those groups through `archives()`, combines their roots in
Stash archive order through `roots()`, and exposes the same occurrences through
a lazily flattened and cached immutable pre-order `List<G>` from `gear()`.

`stash.pull()` delegates to `stash.query(Object.class).pull()`. Because every
stored Gear is an Object and there are no additional criteria, its Batch
reproduces the exact natural hierarchy without lifting.

`whereIf(Class<S>, Predicate<? super S>)` applies its predicate only to Gear of
the named subtype and retains every other selected type. A rejected node's
selected descendants follow the same lifting rule as any other rejected node.
Both predicate methods are in-process Java conveniences, not the later
machine-readable query language: a predicate cannot be described to an
unfamiliar client or serialized across a protocol boundary. Structured criteria
will extend `Query<G>` without changing the Stash/Query/Batch split.

### Fact-derived filters

The model author has already named the collection's filterable concepts through
`@RetroFact`. RetroCrawler therefore does not introduce a second facet registry.
One semantic Fact key produces exactly one `FilterDefinition<T>` across every
Gear class that declares it. Existing same-key validation requires those
declarations to agree on value shape, cardinality, parser, optionality,
strictness, and contextual behavior.

The effective `FactParser<T>` supplies a non-optional `FilterType<T>` after
automatic parser selection has completed. The common parser families define
the useful defaults:

- strings are `Text` filters;
- enums and catalog-backed closed domains are ordered `Choices`;
- integers, instants, and local dates are `Range` filters;
- another parser receives `Exact` equality unless it declares richer
  semantics.

The filter definition holds the key, value type, cardinality, filter type,
applicable Gear types, and framework-owned value extraction. Thus a filter is
defined by the Fact key, not independently for each Gear class.

`RetroCrawler.filters()` exposes the model-wide definitions and, for a choice
filter, every conceivable declared option. `Stash.filters()` and
`Batch.filters()` expose those same definitions. Their
`availability(definition)` operation lazily computes and caches data-set-local
availability:

- every declared choice remains visible, together with its matching Gear
  occurrence count and `present()` predicate;
- a range reports its observed minimum, maximum, and populated occurrence
  count;
- text and exact filters report their populated occurrence count.

The Stash and Batch `FilterDefinitions` contract also exposes
`filters(FilterSelection)`. `ALL` is equivalent to the zero-argument method;
`RELEVANT` retains, in model order, only definitions with at least one populated
Gear occurrence in that data set.

Counts are per Gear occurrence. Repeated equal values on one multi-valued Fact
therefore count once for that option. A Batch derives availability from its
lifted result rather than inheriting the Stash's counts, but it retains absent
declared choices with a zero count.

`Query.where(FilterDefinition<T>, T)` is the first structured criterion. It
requires the selected value for every Gear type to which the Fact applies;
applicable Gear with no value fails the criterion, while Gear types that do not
declare the Fact remain selected. This gives the intended “AGP graphics cards,
but keep unrelated Gear” behavior without naming a Gear subtype in the query.

## Source Provenance and Identity

### Optional @RetroSource

Every resolution requires the source ARI as framework context. The resulting
`GearNode` stores it as the authoritative address of that Gear occurrence.
User Gear classes may optionally ask RetroCrawler to inject the same value:

```java
public final class GraphicsCard {

    @RetroSource
    private ARI source;
}
```

The annotation preserves the bring-your-own-type promise:

- Gear with no `@RetroSource` field remains valid.
- At most one field may be annotated.
- The field type must be `ARI`.
- Injection follows the same writable-field rules as other framework
  injection.
- The value is installed before the completed Gear object becomes visible.

`@RetroSource` is framework context, not a clue or fact. The ARI is already
known from the Artifact being resolved; representing it as an `@ari` clue
would falsely turn framework provenance into observed archive evidence and
duplicate location data.

Parser context carries the current Artifact's authoritative ARI together with
the collection configuration. `ARIParser` resolves artifact-relative resource
observations such as `front.jpeg` directly against that identity. Resource facts
therefore remain ARIs throughout resolution and consumption; no physical or
provider path enters Gear merely to be converted back into an ARI later.

The superseded `ArtifactLocation`, archive-resource `PathParser`, and
`RetroCrawler.identify(ArchiveId, Path)` bridge are removed. Provider paths
remain internal crawling coordinates. The demo and personal collection models
use `ARI` and `Set<ARI>` for photographs and disk images, and consumers pass
those values directly to `RetroCrawler.inspect(...)`.

The ARI-first resource follow-up passes canonical formatting and the full
seven-module `mvn clean install`, including 264 core tests. Focused coverage
includes safe ARI resolution, scalar and collection auto-detection, direct
filesystem and ZIP inspection, provider-independent image facts, cached clues
surviving an archive-root move, and end-to-end demo image resolution.

### ARI and @RetroId are different

An ARI identifies the source Artifact that produced one resolved Gear occurrence
in the current archive arrangement. It is the authoritative address carried by
the source hierarchy and the value optionally injected by `@RetroSource`.

An ARI changes when its folder moves. That is correct: archive location is the
Gear relation. It must not be advertised as permanent physical identity.

`@RetroId` remains the optional, model-defined collector identity that may
survive a move and is unique within the scope validated by a crawl when
present. Gear without a Retro ID remains legitimate.

The path-derived internal `@id` clue likewise remains a convenient location
identity, not proof of enduring physical identity.

No new universal `GearId` is introduced.

## Source Hierarchy

The only Gear hierarchy currently modeled is the one defined by the Artifact
folders. In the motivating collection it expresses “belongs to,” “is stored
inside,” or “is stored alongside.” It carries where Gear is arranged; it never
determines what the Gear is.

The Stash stores the hierarchy as immutable ordered archive groups using the
existing `GearNode<G>` type:

```java
List<ArchiveGear<Object>> archives();
```

Each archive group contains its descriptor and roots. `GearNode` already has
exactly the required Gear, source ARI, and immutable children. A separate
`HierarchyNode` would duplicate that type without adding a concept. No
`Hierarchy<G>` wrapper is warranted until whole-hierarchy behavior exists that
cannot live naturally on the Stash, Query, or Batch.

The hierarchy is heterogeneous because containment and adjacency do not imply
one common Gear type. A computer may contain a graphics card, storage device,
manual, or other unrelated Gear. `Object` honestly represents the complete
forest without imposing a framework base type.

`GearNode` is not merely a presentation result. It is the Stash's canonical
storage unit for a resolved Gear occurrence: Gear, its authoritative source,
and its natural archive-location relation. `pull(...)`, indexes, statistics,
and derived views operate on or project from those nodes. Detailed resolution
evidence may remain in internal state keyed by the node's ARI rather than
bloating the public node.

The source ARI therefore belongs on each `GearNode` even when the Gear class
also uses `@RetroSource`. The annotation mirrors the node's required framework
value for application convenience; it does not become another source of truth.
The node keeps unannotated Gear fully usable.

Vaadin can consume the forest mechanically without a core dependency:

```java
treeGrid.setItems(
        stash.query(MyKnownGear.class).pull().roots(),
        GearNode::children);
```

No general relation engine or alternate hierarchy types are introduced. The
source hierarchy is the archive-location relation already established by the
core model.

## Statistics and Operational Status

Because the Stash contains every recognized Gear, structural statistics can
describe the whole collection:

- number of Gear;
- counts by exact runtime type;
- archive, root, node, and leaf counts;
- maximum source-hierarchy depth.

Source freshness is operational status rather than a single truthful
Stash-wide timestamp. Each archive has its own full-crawl time, and partial
reindexing gives selected subtrees newer timestamps than their ancestors or
siblings.

The API should therefore retain per-archive freshness. It may keep structural
`StashStats` and operational `StashStatus` separate rather than mixing one
misleading “last crawl” value into the statistics.

## Traceability Beyond Native Gear Objects

The native `pull(...)` result deliberately returns the user's Gear objects,
not wrappers. That does not remove Issue 22's traceability requirement.

During resolution RetroCrawler knows more than the current public result keeps:

- the selected Gear type and matching confidence;
- the source Artifact and its raw clues;
- the effective typed facts, each with its source clue and confidence;
- unresolved clues;
- archive crawl timestamps;
- competing equal-confidence Gear matches.

The internal resolved Stash state must retain the information needed for
structured inspection and tracing, indexed by source ARI. It need not place all
of that state on `GearNode` or inject it into the user's object.

The first provenance boundary remains the Artifact. It identifies one archive
location and retains raw clues. Finder identity and exact offsets deliberately
stop at the Artifact/cache boundary today, so the query API must not promise an
exact finder, file, or character position unless the persisted provenance model
is separately extended.

An ordinary Java caller can obtain an ARI from `@RetroSource` or the source
hierarchy and use it to request a trace. A generic machine query result should
carry its source ARI directly.

## Structured Discovery and Query API

The native Stash methods are the Java-friendly start of Issue 22, not the whole
machine-facing API.

Collections are application-defined. An unfamiliar client must be able to
discover:

- collection identity and display name;
- configured archives;
- available Gear types;
- queryable fact keys;
- value shapes, cardinality, and enumerations;
- supported read and lifecycle capabilities.

The structured read API should answer bounded questions such as:

- Describe the collection or one archive.
- Pull or find Gear using explicit criteria.
- Retrieve one occurrence by source ARI.
- Retrieve Gear by a present Retro ID.
- Trace Gear back to its Artifact, clues, facts, and resolution decisions.
- Locate files belonging to matching Gear.
- Report statistics and archive freshness.

The exact names remain open, but the semantic split is firm:

- `pull`, describe, find, retrieve, trace, and locate are reads of one immutable
  Stash;
- access and crawl are the lifecycle commands that obtain or replace the
  crawler's parked Stash.

### Native values versus wire documents

The caller's Gear classes and fact value types are intentionally
application-defined. Serializing arbitrary Gear objects, reflective fields,
parser implementations, or unbounded `Object` values would not provide a
stable protocol.

Java callers should retain typed values. CLI and protocol adapters need a
separate document projection with:

- stable logical identifiers for Gear types and fact keys;
- explicit value shapes and encoders for non-scalar types;
- ARIs for addressable archive resources rather than deployment paths;
- bounded summary results and richer opt-in trace results;
- deterministic ordering.

The current reflected descriptors are useful input but are not themselves a
neutral schema. They expose Java classes, fields, and parser implementations,
and `Model` does not currently retain a public collection description after
construction.

The eventual structured criteria must be representable as data. A Java
`Predicate<G>` remains useful locally but cannot be the only query contract
for JSON, CLI, or agent tools.

## AI and Tool Adapters

The core remains independent of:

- LLM providers and AI SDKs;
- agent protocols such as MCP;
- Vaadin, Spring, and other application frameworks;
- natural-language prompting.

A future adapter may expose tools resembling:

```text
describe_collection
list_archives
find_gear
inspect_gear
trace_gear
locate_files
stash_stats
access_stash
crawl_archive
```

The adapter translates protocol schemas to the public Stash API. It must not
reimplement crawling, resolution, search, or provenance logic.

The agent remains responsible for understanding a natural-language request,
choosing queries, combining results, and writing the answer. RetroCrawler
remains responsible for deterministic collection facts and traceability.

## Existing Factories

`GearTreeFactory`, `StashFactory`, and `FlatListFactory` made the crawl result
caller-shaped while resolution was running. That mechanism helped establish
the source tree, but contradicted the complete Stash retrieval model:

- supplying a Gear type filters before the complete Stash exists;
- tree callbacks expose a mutable construction protocol;
- every result shape causes another full resolution pass;
- a generic caller must already know a suitable Java base class;
- the factory result decides which provenance survives.

The factories and the generic result-producing crawler methods have therefore
been removed. RetroCrawler constructs the complete heterogeneous Stash
directly; `Query<G>` and `Batch<G>` perform every caller-specific projection in
memory afterward.

## Delivery Direction

A useful incremental order is:

1. Make `Stash` non-generic and ensure it contains every recognized Gear from
   every configured archive.
2. Add immutable typed `Query<G>` criteria and materialized lifted `Batch<G>`
   results.
3. Add optional `@RetroSource` injection while keeping ARI out of clues and
   facts.
4. Expose the heterogeneous source forest by archive and adapt Vaadin and CLI
   callers.
5. Complete access/crawl lifecycle details and add whole-Stash statistics plus
   per-archive status.
6. Preserve resolution evidence in the Stash and add exact retrieval
   and trace access by ARI and present Retro ID.
7. Expose neutral collection schema, value documents, and structured query
   criteria.
8. Add machine-readable CLI commands.
9. Add a thin MCP or other agent adapter outside core.

Each slice should keep the existing public defaults usable where practical and
include focused tests for new contracts.

## Decisions

1. `Stash`, not `RetroCrawler`, is the normal user-facing interface to
   resolved Gear.
2. A Stash contains all recognized Gear eagerly; type selection never triggers
   resolution.
3. `Stash` is not generic. The desired return type belongs to
   `query(Class<G>)`.
4. `Query<G>` is an immutable selection recipe tied to one Stash snapshot.
5. `Batch<G>` is the immutable typed materialization. It retains archive groups,
   exposes their cumulative lifted roots, and lazily derives and caches its flat
   pre-order Gear list. It performs no further filtering.
6. `stash.pull()` is the all-Gear shorthand and returns `Batch<Object>`.
7. No `GearEntry` wrapper is added.
8. Every Gear resolution receives an ARI as required framework context;
   `@RetroSource` optionally mirrors it into a Gear object without creating a
   clue or fact.
9. `GearNode` is the canonical stored Gear occurrence and the node of the
   heterogeneous immutable source forest; neither `HierarchyNode` nor a
   `Hierarchy` wrapper is added.
10. Archive location remains the only Gear relation. The hierarchy never
   determines Gear type.
11. Stashes, Queries, Batches, and hierarchy forests are immutable views of one
    snapshot; a successful crawl parks a new Stash without changing old ones.
12. The Repository continues to store clue archives, not resolved Stash state.
13. Native Java values and machine-readable query documents are separate
    boundaries.
14. Natural-language interpretation and protocol integrations remain outside
    core.

## Relationship to Other Issues

### Issue 17

Issue 17 makes storage of extracted clue archives replaceable. That Repository
is an internal source used while preparing the Stash; it is not the Gear query
API or a store of resolved Gear.

### Issue 21

Issue 21 determines how the model, repository, sources, and extensions are
configured and assembled. Issue 22 determines how a configured engine exposes
the resulting Stash to consumers.

Together:

- Issue 17: where extracted clue archives are stowed away and retrieved;
- Issue 21: how the RetroCrawler engine is configured;
- Issue 22: how users communicate with their Stash and explore Gear.

### Issue 12 and Issue 24

Equal-confidence Gear matching and explainable resolution remain prerequisites
for trustworthy trace results. Issue 22 should expose their outcomes without
silently presenting a first-wins match as certain.

## Implementation Outline

- [x] Audit the current crawl/factory retrieval path and where provenance is
      lost.
- [x] Reconcile the later Stash, batch, source, and hierarchy concept
      discussion.
- [x] Settle the native vocabulary and absence of `GearEntry` and `Hierarchy`
      wrapper types.
- [x] Make the Stash complete, heterogeneous, non-generic, immutable, parked by
      RetroCrawler, and user-facing.
- [x] Add immutable typed Query criteria and lifted Batch materialization.
- [x] Add and validate optional `@RetroSource` while requiring ARI throughout
      resolution.
- [x] Expose the source hierarchy forest by archive and adapt Vaadin and CLI.
- [x] Establish `access` versus physical `crawl` and atomic Stash parking.
- [ ] Expand statistics and per-archive freshness/status.
- [ ] Retain resolution evidence and add retrieval/trace access.
- [ ] Define collection and query-schema discovery.
- [ ] Define protocol-safe value documents and structured query criteria.
- [ ] Add focused tests for native access, hierarchy, provenance, lifecycle,
      search, tracing, and serialization.
- [ ] Add JSON-oriented CLI commands.
- [ ] Add a thin AI/protocol adapter outside core.

### Implementation update (2026-08-29)

- `RetroCrawler` now exposes `access(Journal)` and
  `crawl(Journal, ReindexScope)` as its only Stash-producing operations.
- It parks one immutable complete Stash. Hot access returns that instance;
  successful physical crawls atomically park a new instance, while old Stashes
  remain unchanged.
- `Stash` is non-generic and stores every resolved occurrence as an
  archive-grouped `GearNode<Object>` forest.
- `stash.pull()` returns `Batch<Object>`;
  `stash.query(Type.class).where(archiveIds).where(predicate).pull()` returns a
  typed immutable lifted `Batch<G>` with archive-grouped, cumulative-root, and
  lazily cached flat Gear views.
- Fact parsers now expose non-optional structured filter semantics. One
  `FilterDefinition` is derived per Fact key and shared by every declaring Gear
  type; enum and catalog parsers expose global choices, string parsers expose
  text, integer and temporal parsers expose ranges, and other parsers default
  to exact equality.
- RetroCrawler, Stash, and Batch expose model filters. Stash and Batch lazily
  cache local `FilterAvailability`, retaining zero-count global choices, and
  Query accepts exact Fact-value criteria without eliminating non-applicable
  Gear types. Stash and Batch share the `FilterDefinitions` discovery contract,
  including `filters(ALL)` and the data-set-local `filters(RELEVANT)` selection.
- Focused tests cover the four filter shapes, same-key reuse across Gear types,
  global-versus-present choices, lazy caching, and structured Query behavior.
  The complete seven-module reactor passes 390 tests.
- The crawl-time `GearTreeFactory`, `StashFactory`, and `FlatListFactory`
  projection path was removed. Vaadin, CLI, demo, core, and collection callers
  now project from Stash through Query and Batch.
- Canonical formatting passes for all 24 changed Java source files. The focused
  core suite passes 265 tests, and the complete seven-module reactor passes 386
  tests.

## Remaining Design Questions

- Which range, text, negation, and multi-choice operations should follow the
  first exact Fact-value criterion on native `Query<G>`?
- What hierarchy-building strategy should the future `pull(...)` overload
  accept while keeping the default lifted source hierarchy?
- How should equal-confidence Gear matches and partially resolved Gear be
  represented?
- How much evidence should ordinary inspection return, and how much belongs
  only in an explicit trace?
- Which fact value types receive built-in wire encodings, and how does a model
  register schema and encoding for a custom type?
- Which explicit metadata is needed for stable Gear type identifiers and
  human-readable schema?
- Should structured criteria use one data-only query model with a typed Java
  builder, or another arrangement?
- Where do sorting, pagination, and result limits enter structured queries?
- Should machine-readable commands remain in the demonstration CLI or move to
  a dedicated adapter module?
- What lifecycle and module boundary should a future MCP adapter use?

## Out of Scope

- A persisted database of resolved Gear.
- Embeddings or a vector database as the first query implementation.
- A mandatory Gear interface or superclass.
- A mandatory `@RetroSource` field.
- A `Batch` wrapper that contains only a plain immutable list and loses the
  hierarchy or source occurrence.
- A `GearEntry` wrapper duplicating Gear plus ARI.
- A `Hierarchy` wrapper without whole-hierarchy behavior.
- Alternate relation types beside archive location.
- Putting an LLM, AI SDK, or protocol dependency in core.
- Accepting natural-language questions as the core query API.
- Replacing deterministic clues, facts, and resolution with probabilistic
  extraction.
- Treating the persisted clue cache as the public query model.
- Removing the Vaadin sample application.
