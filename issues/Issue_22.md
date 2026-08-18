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
on `GearEntry`, a Java `Batch` type, a public `StashSnapshot`, a
`FindingAid` facade, or a `Hierarchy` wrapper.

The settled vocabulary is:

- `RetroCrawler` is the configured crawling and resolution engine.
- `Stash` is the long-lived, user-facing interface to all currently recognized
  Gear.
- A user **pulls a batch** of one assignable Gear type from the Stash.
- That batch is an immutable `List<G>`; “batch” is domain language, not a Java
  type.
- The source hierarchy is an immutable forest of `GearNode<Object>`.
- Every hierarchy node has an authoritative source ARI.
- A Gear object may optionally receive that ARI through `@RetroSource`.

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
   pull batch  hierarchy  structured query/trace
     List<G>    forest       documents
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

Stash stash = crawler.stash();
```

The exact initial-refresh signature remains to be designed, but the ownership
is settled: the Stash is the communication object between a user and their
Gear, while RetroCrawler supplies its engine.

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

- Matching and construction failures occur while refreshing the Stash rather
  than during an unrelated later pull.
- Retro ID uniqueness is validated across the complete selected collection.
- Every Artifact is parsed and matched once per refresh, not once per Gear type.
- Whole-collection statistics actually describe everything currently known.
- A batch is a predictable in-memory selection.

Expensive resource access remains deferred. Images, manuals, disk images, and
other file content are still inspected through their ARIs only when requested.

The resolved Stash is not persisted as another source of truth. The
`Repository` continues to store only rebuildable, model-independent clue
archives so model changes can reinterpret them without reindexing.

### Stash state and lifecycle

The Stash is long-lived, but each installed resolved state is immutable. A
refresh or reindex builds and validates a complete candidate before atomically
replacing the current state:

```text
current state -> build candidate -> validate -> install candidate
                       |
                       '-> failure: retain current state
```

Lists and hierarchy roots already returned to a caller remain immutable views
of the state from which they were pulled; a later refresh does not silently
change them. This does not require a public `StashSnapshot` type initially.
The implementation may keep such a state object internally and introduce it
publicly only if a concrete use case needs explicit revision handling.

Lifecycle vocabulary should distinguish:

- **refresh** — rebuild the complete resolved state, reusing stored clue
  archives and crawling only where no usable stored archive exists;
- **reindex** — explicitly reread the requested archive scope before resolving
  a new complete state;
- **pull/query** — pure reads of the installed state that never crawl or
  resolve.

`rebuild` is avoided because it does not say whether clues are being reused,
sources are being reread, or only Gear is being resolved again. Refresh and
reindex are visibly state-changing operations even if they live on the
user-facing Stash facade.

## Pulling a Batch

The first native retrieval API should be deliberately small:

```java
<G> List<G> pull(Class<G> gearType);

<G> List<G> pull(
        Class<G> gearType,
        Predicate<? super G> filter);
```

For example:

```java
List<GraphicsCard> cards =
        stash.pull(GraphicsCard.class);

List<GraphicsCard> workingCards =
        stash.pull(GraphicsCard.class, GraphicsCard::isWorking);
```

The returned list is the batch pulled from the Stash. A dedicated `Batch<G>`
class would add no value at this stage:

- `List<G>` already provides the requested type.
- `List.copyOf(...)` provides immutable snapshot behavior.
- Predicates and streams provide ordinary Java filtering and grouping.
- Whole-collection statistics belong to the Stash.
- Hierarchy is an orthogonal view of the complete Stash, not an intrinsic
  property of a typed flat selection.

RetroCrawler does not require a common Gear base class. Selection therefore
uses assignability:

```java
gearType.isInstance(gear)
```

`pull(GraphicsCard.class)` includes instances of `GraphicsCard` and its
subclasses. A caller that deliberately wants every recognized object can use:

```java
List<Object> everything = stash.pull(Object.class);
```

No synthetic Gear interface or special “all Gear” token is needed.

The Java predicate overload is a convenience for in-process callers. It is not
the later machine-readable query language: a predicate cannot be described to
an unfamiliar client or serialized across a protocol boundary.

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

The hierarchy is exposed as an immutable, ordered forest using the existing
`GearNode<G>` type:

```java
List<GearNode<Object>> sourceHierarchy();
```

The outer list contains the roots. `GearNode` already has exactly the required
Gear, source ARI, and immutable children. A separate `HierarchyNode` would
duplicate that type without adding a concept. No `Hierarchy<G>` wrapper is
warranted until whole-hierarchy behavior exists that cannot live naturally on
the Stash or be derived by traversal.

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
        stash.sourceHierarchy(),
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

- `pull`, describe, find, retrieve, trace, and locate are reads of installed
  Stash state;
- refresh and reindex are explicit lifecycle commands that prepare and install
  another complete state.

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
refresh_stash
reindex_archive
```

The adapter translates protocol schemas to the public Stash API. It must not
reimplement crawling, resolution, search, or provenance logic.

The agent remains responsible for understanding a natural-language request,
choosing queries, combining results, and writing the answer. RetroCrawler
remains responsible for deterministic collection facts and traceability.

## Existing Factories

`GearTreeFactory`, `StashFactory`, and `FlatListFactory` currently make the
crawl result caller-shaped while resolution is running. That mechanism helped
establish the source tree, but it is not the new user-facing retrieval model:

- supplying a Gear type filters before the complete Stash exists;
- tree callbacks expose a mutable construction protocol;
- every result shape causes another full resolution pass;
- a generic caller must already know a suitable Java base class;
- the factory result decides which provenance survives.

The complete Stash should become the canonical result first. `pull(...)` then
replaces the ordinary flat-list factory use case, and `sourceHierarchy()`
replaces UI-specific tree construction for the source hierarchy.

Existing factory methods may remain temporarily for compatibility or as
implementation machinery. A new generic projection abstraction should not be
introduced until a concrete result shape exists that cannot be built from the
immutable batch and hierarchy APIs.

## Delivery Direction

A useful incremental order is:

1. Make `Stash` non-generic and ensure its installed state contains every
   recognized Gear from every selected archive.
2. Add immutable, assignable `pull(Class)` and `pull(Class, Predicate)`
   operations.
3. Add optional `@RetroSource` injection while keeping ARI out of clues and
   facts.
4. Expose the heterogeneous source forest as `List<GearNode<Object>>` and
   adapt Vaadin and CLI callers.
5. Define refresh/reindex lifecycle, atomic state replacement, and
   whole-Stash statistics plus per-archive status.
6. Preserve resolution evidence in the installed state and add exact retrieval
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
3. `Stash` is not generic. The requested Gear type belongs to `pull(...)`.
4. Users pull a batch, but the batch is an immutable `List<G>`, not a
   `Batch<G>` class.
5. `Object.class` is the explicit all-Gear selection when no common user base
   class exists.
6. No `GearEntry` wrapper is added.
7. Every Gear resolution receives an ARI as required framework context;
   `@RetroSource` optionally mirrors it into a Gear object without creating a
   clue or fact.
8. `GearNode` is the canonical stored Gear occurrence and the node of the
   heterogeneous immutable source forest; neither `HierarchyNode` nor a
   `Hierarchy` wrapper is added.
9. Archive location remains the only Gear relation. The hierarchy never
   determines Gear type.
10. Returned batches and hierarchy forests are immutable views of one installed
    Stash state.
11. The Repository continues to store clue archives, not resolved Stash state.
12. Native Java values and machine-readable query documents are separate
    boundaries.
13. Natural-language interpretation and protocol integrations remain outside
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
- [x] Settle the native vocabulary and absence of `GearEntry`, `Batch`, and
      `Hierarchy` wrapper types.
- [ ] Make the Stash complete, heterogeneous, non-generic, and user-facing.
- [ ] Add typed immutable pull operations.
- [x] Add and validate optional `@RetroSource` while requiring ARI throughout
      resolution.
- [ ] Expose the source hierarchy forest and adapt existing clients.
- [ ] Define lifecycle and atomic state replacement.
- [ ] Expand statistics and per-archive freshness/status.
- [ ] Retain resolution evidence and add retrieval/trace access.
- [ ] Define collection and query-schema discovery.
- [ ] Define protocol-safe value documents and structured query criteria.
- [ ] Add focused tests for native access, hierarchy, provenance, lifecycle,
      search, tracing, and serialization.
- [ ] Add JSON-oriented CLI commands.
- [ ] Add a thin AI/protocol adapter outside core.

## Remaining Design Questions

- Does obtaining `crawler.stash()` perform the initial refresh, or is the
  initial lifecycle command explicit?
- What are the exact refresh and reindex signatures, return values, and
  concurrency semantics?
- How should archive scoping and archive display metadata accompany the flat
  list of source-hierarchy roots?
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
- A Java `Batch` wrapper for a plain immutable list.
- A `GearEntry` wrapper duplicating Gear plus ARI.
- A `Hierarchy` wrapper without whole-hierarchy behavior.
- Alternate relation types beside archive location.
- Putting an LLM, AI SDK, or protocol dependency in core.
- Accepting natural-language questions as the core query API.
- Replacing deterministic clues, facts, and resolution with probabilistic
  extraction.
- Treating the persisted clue cache as the public query model.
- Removing the Vaadin sample application.
