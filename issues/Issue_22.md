# Issue 22: Add a Structured Query API for Human and AI Collection Exploration

## Context

RetroCrawler was originally expected to culminate primarily in a Vaadin
application. The arrival of capable AI agents changes the most useful operating
environment for the project: RetroCrawler can become the collection-intelligence
toolkit through which an agent quickly and reliably answers questions about a
collector's gear.

This does not make the Vaadin sample application obsolete. It remains valuable
as a visual collection browser, demonstration, and integration check. It should
eventually consume the same public query API as every other client rather than
define the architecture itself.

## Intent

Provide a structured, discoverable query API through which humans,
applications, command-line tools, and AI agents can explore a RetroCrawler
collection.

RetroCrawler should perform the deterministic work:

- Crawl the filesystem archive.
- Extract clues and facts.
- Resolve those facts into gear.
- Retain the evidence and provenance behind the result.
- Answer explicit structured queries about the collection.

An AI agent may interpret a person's natural-language question and translate it
into those queries. Natural-language interpretation itself does not belong in
`retro-crawler-core`.

A concise statement of the direction is:

> RetroCrawler turns collectors' filesystem archives into structured,
> traceable gear that humans, applications, and AI agents can explore.

## Architectural Direction

```text
Collectors' filesystem archives
              |
              v
     crawling and resolution
              |
              v
 structured, traceable collection model
              |
              v
       public query API
        /      |       \
       /       |        \
    CLI     AI adapter   Vaadin
```

The query API is the product boundary. Consumers should not need to understand
RetroCrawler's internal assembly and resolution classes in order to inspect a
collection.

The core must remain independent of:

- A particular LLM or AI provider.
- AI SDKs.
- Agent protocols such as MCP.
- Vaadin, Spring, or another application framework.
- Natural-language prompting.

AI and protocol support should be implemented as thin adapters over the public
API, preferably in separate modules.

## Why the Existing Model Is a Good Foundation

RetroCrawler already models several concepts that are useful for trustworthy
collection answers:

- `Gear` gives collection entries a deliberately broad domain abstraction.
- Stable IDs allow a particular archive or piece of gear to be addressed.
- Clues and facts retain more meaning than an opaque search index.
- Confidence and unresolved attributes can explain uncertainty.
- `Repository` avoids repeating expensive extraction work.
- `GearTreeFactory` lets callers choose an application-specific output shape.

The important gap is that the final gear object alone is not necessarily a
complete explanation. Resolution may discard or hide the archive path, clues,
facts, confidence, ambiguity, and unused attributes that justified the object.
Those details are essential when a human or agent needs to verify an answer.

## Traceable Results

The query layer should expose a neutral, machine-readable representation of
resolved gear while preserving its evidence. A conceptual result might look
like:

```java
public interface ResolvedGear<G> {

	G gear();

	GearId gearId();

	ArchiveId archiveId();

	Path sourcePath();

	Map<String, ?> attributes();

	Artifact artifact();

	Confidence confidence();
}
```

This is a sketch, not a settled public type. The actual model must be derived
from the existing archive, clue, fact, artifact, and gear types. It may need to
represent multiple source paths, unresolved attributes, individual evidence
records, and ambiguous candidates rather than the simplified fields above.

The essential contract is that callers can:

- Obtain a useful structured description of the gear.
- Identify the archive and source material from which it was resolved.
- Inspect the clues and facts that support its attributes.
- See uncertainty, ambiguity, and information that was not consumed.
- Serialize results without serializing arbitrary internal implementation
  objects.

## Query Capabilities

The first public read API should answer bounded, structured questions such as:

- List the available archives.
- Describe an archive or the overall collection.
- Find gear using structured criteria.
- Retrieve one piece of gear by its stable ID.
- Trace a piece of gear back to its paths, clues, facts, and resolution
  decisions.
- Locate files belonging to matching gear.

Reindexing is also useful to tools, but it is a command with side effects rather
than a read query and should remain visibly distinct.

A conceptual surface could include operations resembling:

```text
listArchives
describeCollection
describeArchive
findGear
getGear
traceGear
locateFiles
reindexArchive
```

These names and their grouping are not yet final. In particular, the design
must decide whether reads and commands belong to one facade or separate public
interfaces.

## Discovery Before Querying

Collections are application-defined. An AI adapter cannot assume that every
collection contains computers, consoles, books, or the same gear attributes.
It therefore needs a way to discover:

- Which archives exist.
- Which gear types are available.
- Which attributes and identifiers can be queried.
- What value shapes or enumerations those attributes use.
- Which capabilities the current crawler configuration exposes.

`describeCollection` is therefore a foundational operation, not merely a
presentation convenience. It provides the schema and vocabulary needed by a
generic client before that client constructs more specific queries.

## AI and Tool Adapters

A future AI-facing adapter could present narrowly scoped tools such as:

```text
list_archives
describe_collection
find_gear
inspect_gear
trace_clues
locate_files
reindex_archive
```

The adapter should translate between a protocol's input/output schema and
RetroCrawler's public API. It should not reimplement crawling, resolution, or
query behavior.

The agent remains responsible for understanding a natural-language request,
selecting the appropriate tools, combining their structured results, and
writing the human answer. RetroCrawler remains responsible for collection
facts and their provenance.

An MCP adapter is a plausible first agent integration, but MCP must not leak
into the core API. The query layer should be equally usable by an MCP server, a
different agent protocol, or ordinary Java application code.

## Delivery Direction

A useful incremental order is:

1. Preserve provenance through gear resolution.
2. Define a neutral, machine-readable representation of resolved gear.
3. Expose collection and schema discovery.
4. Add the structured read query API.
5. Expose the API through machine-readable CLI commands, such as JSON input and
   output.
6. Add a separate MCP or other AI-tool adapter.
7. Move the Vaadin application onto the same query API.

The JSON CLI is useful before a dedicated agent adapter because it exercises
the public boundary, supports scripting, and provides an immediately available
integration surface without introducing an AI dependency.

## Relationship to Other Issues

### Issue 17

Issue 17 makes storage of extracted clue archives replaceable. That repository
is an internal source used while preparing queryable collection data; it is not
itself the collection query API.

### Issue 21

Issue 21 provides a unified builder for annotation-based and manual
configuration. It determines how a crawler and its tools are assembled. Issue
22 determines how a configured crawler exposes its collection to consumers.

Together the responsibilities are:

- Issue 17: where extracted archives are stowed away and retrieved.
- Issue 21: how the RetroCrawler toolset is configured and assembled.
- Issue 22: how consumers explore the resulting collection.

## Implementation Outline

- [ ] Audit where provenance is currently lost between clues, facts, resolved
      gear, and `GearTreeFactory`.
- [ ] Define the minimum stable identity required for archives and gear in
      query results.
- [ ] Design a serializable, implementation-neutral resolved-gear result.
- [ ] Represent source paths, supporting evidence, confidence, ambiguity, and
      unresolved attributes.
- [ ] Define collection and query-schema discovery.
- [ ] Define structured criteria for finding gear without accepting arbitrary
      natural-language strings.
- [ ] Introduce a small public read-query facade.
- [ ] Keep reindexing and other mutations explicit and separate from reads.
- [ ] Add focused tests for discovery, searching, retrieval, tracing, and
      serialization.
- [ ] Add JSON-oriented CLI commands over the query API.
- [ ] Add a thin AI/protocol adapter in a separate module.
- [ ] Adapt the Vaadin sample application to the same API.
- [ ] Document how an application or agent can inspect an unfamiliar
      collection before querying it.

## Open Design Questions

- What should identify gear whose user-defined class does not currently expose
  a globally unique ID?
- Should the neutral result be a set of records, interfaces, or a serializable
  document model?
- How much of the raw clue tree should a normal query return, and how much
  should only a trace operation expose?
- How should competing gear matches and partially resolved gear be represented?
- Should query criteria be a typed Java DSL, a simple filter model, or both?
- Can query schema be derived entirely from configured gear descriptors, or
  does it need explicit metadata?
- Should the read facade operate on one configured archive or aggregate
  multiple archives?
- Where should pagination, sorting, and result limits enter the API?
- Should machine-readable CLI commands be added to the existing CLI module or
  isolated from the demonstration commands?
- What is the appropriate module boundary and lifecycle for a future MCP
  adapter?

## Out of Scope

- Embeddings or a vector database as the first query implementation.
- Putting an LLM or AI SDK in `retro-crawler-core`.
- Accepting a large natural-language question as the core query API.
- Replacing deterministic clues, facts, and resolution with probabilistic
  extraction.
- Removing the Vaadin sample application.
- Treating the persisted extraction cache as the public query model.
