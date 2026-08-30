# Issue 37: Implement a RetroCrawler AI-Friendly Interface

## Intent

Make a running RetroCrawler installation explorable and controllable by AI
agents without coupling the framework to an LLM vendor or embedding an LLM in
RetroCrawler.

The initial deployment target is a Docker container on a QNAP NAS. It should:

- retain the existing ability to crawl the collection on a nightly schedule;
- let an authorized agent query the most recently published collection state;
- let an authorized agent initiate and monitor a crawl; and
- work with MCP-capable clients such as Codex, Claude, and suitable local-LLM
  hosts.

MCP is the agent-facing protocol. RetroCrawler's core remains a deterministic,
transport-neutral collection toolkit.

## Architectural Boundaries

The durable module split is:

```text
retro-crawler-server ──> retro-crawler-mcp ──> retro-crawler-core

collection module ───────────────────────────> retro-crawler-core/model
```

At runtime, the server and exactly one collection-specific composition are
present in the same Spring application context. For the initial deployment the
collection module is `retro-crawler-mycollection`.

### `retro-crawler-core`

- Owns crawling, archive access, progress, cancellation, and structured query
  contracts.
- Owns the transport-neutral asynchronous crawl-operation lifecycle used by
  protocol adapters and scheduled hosts.
- Exposes machine-readable results with provenance.
- Has no Spring, MCP, or AI SDK dependency.
- Provides the same public API to MCP, CLI, Vaadin, and future consumers.

Issue #22 already established the transport-neutral read side needed here:

- `RetroCrawler.access(...)` returns the parked latest successful `Stash` and
  reconstructs it from stored clues after a restart;
- `RetroCrawler.crawl(...)` builds a complete candidate and parks it only after
  successful crawl, resolution, and validation;
- `Stash`, `Query`, and `Batch` expose the complete collection, typed
  selections, archive grouping, and hierarchy;
- every `GearNode` carries its authoritative source ARI and optional
  `ResolutionTrace`; and
- the trace retains the artifact, original clues and their provenance,
  resolved facts and confidence, matcher decisions, and non-fatal resolution
  issues.

Issue #37 must adapt those existing contracts to bounded MCP response DTOs. It
must not introduce another snapshot, duplicate Stash lifecycle, reflect over
arbitrary gear objects, or serialize their `toString()` representations.

### `retro-crawler-mcp`

- Is a reusable Spring Boot auto-configuration library, not an executable
  application.
- Depends on `retro-crawler-core`, the Spring MCP annotation API, and Spring
  Boot auto-configuration support.
- Does not select an MCP transport; the executable host supplies the appropriate
  Spring MCP server starter.
- Knows nothing about `retro-crawler-mycollection` or any archive paths.
- Registers MCP tool beans when a suitable `RetroCrawler` bean is present.
- Auto-configures one replaceable `CrawlOperationService` bean around that
  crawler so MCP crawl control and server scheduling can share it.
- Maps core request, result, progress, and failure types to conservative JSON
  schemas.
- Uses MCP tool annotations and hints to identify read-only and mutating
  operations.

Auto-configuration is registered through Spring Boot's
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
Application beans remain replaceable with `@ConditionalOnMissingBean` where an
extension point is useful.

### `retro-crawler-server`

- Is the generic executable Spring Boot host.
- Depends on `retro-crawler-mcp`, but has no compile-time dependency on a
  particular collection.
- Selects Spring's WebMVC MCP server starter as its HTTP transport.
- Does not discover or construct collection models itself.
- Owns external location binding and the conventional composition of a model,
  JSON clue repository, and filesystem archives into a crawler.
- Requires exactly one `RetroCrawler` bean in its application context and fails
  clearly at startup when none or several are present.
- Supplies the HTTP runtime, operational configuration, security, health, and
  graceful shutdown concerns.
- Initially exposes MCP over Streamable HTTP at `/mcp`.

This is a permanent host boundary rather than a temporary collection-specific
server.

### Collection Model and Server Composition

The collection integration supplies what the collection means; the generic
server supplies where it is. For the initial deployment,
`retro-crawler-mycollection` provides only its `Model`:

```java
@AutoConfiguration
@ConditionalOnMissingBean(Model.class)
public class MyCollectionAutoConfiguration {

	@Bean
	Model model() {
		return Model.from("com.retrocrawler.mycollection");
	}
}
```

The server binds the external values into `LocationsProperties`, converts them
to core's framework-neutral `Locations`, and performs the conventional
composition:

```java
@Bean
RetroCrawler retroCrawler(final Model model, final LocationsProperties properties) {
	return RetroCrawler.builder()
			.model(model)
			.locations(properties.toLocations())
			.build();
}
```

`Locations` deliberately pairs with `Model`: the model says what the
collection means, while locations say where its archives and rebuildable clue
repository are. `Builder.locations(...)` selects `JsonFileRepository` and
filesystem archive sources. The existing explicit `repository(...)` and
`archive(..., source)` methods remain the path for non-standard storage.

This module may depend on Spring Boot auto-configuration support, but it does
not depend on `retro-crawler-mcp` or `retro-crawler-server`. A non-Spring
consumer can continue to use its model classes directly.

The external configuration is generic and supports several archives:

```properties
retro-crawler.repository.root=/repository

retro-crawler.archives[0].id=hardware
retro-crawler.archives[0].name=Hardware
retro-crawler.archives[0].root=/archives/hardware

retro-crawler.archives[1].id=software
retro-crawler.archives[1].name=Software
retro-crawler.archives[1].root=/archives/software
```

The Docker image combines the generic server artifact with the selected
collection JAR and its runtime dependencies on the extension classpath. A
different collection can therefore replace `retro-crawler-mycollection`
without rebuilding or changing the generic server. The server uses Spring
Boot's `PropertiesLauncher`; its `LOADER_PATH` points to the directory holding
those extension libraries.

## MCP Tool Surface

The first interface should be deliberately small and use standard MCP
`tools/list` and `tools/call` behavior:

- `list_archives` — list registered archive identities and current published
  crawl metadata.
- `search_gear` — search the parked Stash with pagination
  and bounded result sizes.
- `get_gear` — retrieve one piece of gear and its traceable facts and clues.
- `browse_stash` — browse a bounded part of the archive/gear hierarchy.
- `start_crawl` — start an asynchronous crawl and return an operation ID.
- `get_crawl` — return progress, state, failures, timing, and completion data.
- `cancel_crawl` — request cancellation of an active crawl.

Tool results should provide structured JSON plus a concise text representation
for clients with weaker structured-content support. Input schemas should avoid
client-specific extensions.

## Crawl and Publication Semantics

Core already guarantees serialized `access`/`crawl` production, immutable
Stashes, and atomic replacement of the current Stash after success. While a new
crawl is running, readers can continue using the previously parked Stash. A
failed or cancelled crawl never replaces it.

`CrawlOperationService` provides asynchronous command semantics around that
core contract:

- Crawl initiation is asynchronous; an MCP request must not remain open for a
  complete crawl.
- Only one crawl operation may be active; another start is rejected with the
  active operation ID.
- `start_crawl` accepts registered archive identities and validated ARIs, never
  arbitrary filesystem paths.
- Operation progress is based on `Journal` and `ProgressSnapshot` and is
  addressable by an opaque operation ID.
- Cancellation is cooperative and distinguishes `CANCELLING` from the terminal
  `CANCELLED` state.
- Successful, failed, and cancelled operations remain available through a
  bounded in-memory history with stable failure descriptions.
- Queries obtain the immutable latest-successful Stash through
  `RetroCrawler.access(...)`.
- Process restart should reconstruct the published view from the repository
  without forcing a filesystem re-index when cached clues remain valid.

The optional server scheduler uses the configured cron expression and time
zone to submit `ReindexScope.all()` to that same service. It is disabled by
default. If a manual or earlier scheduled crawl still occupies the service,
the trigger is logged and skipped; no second crawl is queued.

The explicit `start_crawl` / `get_crawl` / `cancel_crawl` lifecycle is preferred
over relying on newer optional MCP task primitives so the interface remains
portable across clients.

## Security and Operational Constraints

- The service is intended for a trusted LAN or VPN and must still require
  authentication.
- Read operations and crawl-control operations should have separate
  authorization scopes.
- No tool may accept arbitrary paths, execute shell commands, or modify archive
  contents.
- Query inputs require pagination and server-side limits.
- Clue and archive text is untrusted data and must never be treated as agent
  instructions.
- Spring MCP server authentication is not assumed; the Boot host must configure
  it explicitly.
- Archive mutation, file upload, rename, and deletion tools are outside this
  issue.

## Decisions

1. Use MCP as the primary AI-facing protocol.
2. Use Streamable HTTP rather than an STDIO-only server for the NAS deployment.
3. Keep AI, Spring, and MCP dependencies outside `retro-crawler-core`.
4. Introduce `retro-crawler-mcp` immediately as a reusable adapter and Boot
   auto-configuration module.
5. Introduce `retro-crawler-server` as the generic executable host, not as a
   collection-specific application.
6. Make the server require exactly one `RetroCrawler` bean. By default the
   server constructs it; an application-provided crawler remains an override.
7. Let the selected collection integration supply exactly one `Model` for the
   server's default crawler composition.
8. Do not make the server or MCP module depend on
   `retro-crawler-mycollection`.
9. Compose the generic server and selected collection at runtime in the Docker
   image.
10. Keep query and crawl-control tools separate and expose asynchronous crawl
    progress explicitly.
11. Rely on core's Stash lifecycle for atomic publication and retention of the
    previous successful view on failure or cancellation.
12. Do not expose arbitrary filesystem or shell access.
13. Keep the existing Docker Compose deployment and use
    `application.properties` for Spring application configuration.
14. Collection-specific configuration represents multiple archives as an
    indexed `archives` list. Each archive has its own `id`, `name`, and `root`.
15. Keep collection deployment properties under the generic `retro-crawler`
    namespace rather than embedding a module name such as `mycollection` in the
    external contract. The selected collection JAR supplies the model; the
    configured values describe its deployment.
16. Configure the shared repository location symmetrically as
    `retro-crawler.repository.root`; it is the writable root for the
    rebuildable clue repository, not an archive source root.
17. Put the framework-neutral `Locations` value in `retro-crawler-core` and add
    `RetroCrawler.Builder.locations(...)` as the conventional JSON-repository
    and filesystem-archive composition path.
18. Bind external archive and repository paths in the server-owned
    `LocationsProperties` bean. Keep operational settings in the independent
    `RetroCrawlerServerProperties` bean.
19. Package the server with Spring Boot's `ZIP` layout so
    `PropertiesLauncher` can add an external collection and its runtime
    dependencies through `LOADER_PATH`.
20. Treat exactly one `RetroCrawler` as a requirement of the MCP adapter as
    well as the server. Creating the tool bean through normal dependency
    injection makes composition independent of auto-configuration order.
21. Record crawl scheduling under `retro-crawler.server.crawl.*`, default it
    to disabled, and make the scheduled path and `start_crawl` share one
    asynchronous operation service.
22. Use the non-generic Stash introduced by issue #22 directly. A collection
    extension does not need to publish a root Gear type for server startup.
23. Treat core's crawl lock and atomic Stash replacement as the collection
    consistency mechanism. The core operation service adds admission,
    progress, cancellation, and status policy; it does not own another Stash.
24. Put `CrawlOperationService` and its immutable lifecycle values in the
    focused `com.retrocrawler.core.crawl` package so they remain
    transport-neutral without crowding core's root API. Let the MCP
    auto-configuration publish one replaceable Spring bean that the later MCP
    tools and server scheduler share.

## Initial Module Scaffold

The first implementation step establishes both module boundaries in the Maven
reactor:

- Spring Boot 4.1.0 and Spring AI 2.0.1 version properties live in the reactor
  parent, while their BOMs remain scoped to the Spring-based modules so they do
  not alter dependency resolution in `retro-crawler-core`.
- `retro-crawler-mcp` requires exactly one `RetroCrawler` when creating its
  default tools bean and backs off for an application-provided tools bean.
- It also auto-configures one `CrawlOperationService`, with graceful shutdown,
  and backs off for an application-provided service.
- `list_archives` is the first real MCP tool. It returns collection and archive
  identities without exposing physical archive roots.
- `retro-crawler-server` selects synchronous Streamable HTTP and is packaged as
  an executable Spring Boot JAR.
- Its optional scheduler creates a `CronTrigger` from the bound server
  properties and starts full crawls through the shared operation service.
- The server's own configuration requires exactly one `RetroCrawler`; missing
  and ambiguous crawler configurations fail during context startup.
- The packaged server contains `retro-crawler-mcp` but no collection-specific
  module.
- `retro-crawler-mycollection` auto-configures only its `Model`; the server
  turns that model and the configured `Locations` into the crawler.
- The resulting crawler already owns its current immutable Stash and exposes
  it through `access`; neither the server nor MCP module adds a snapshot
  holder.
- `application.example.properties` documents the external contract without
  baking one deployment's paths into the server artifact.

## Open Questions

- What bounded MCP DTO shape best projects `Batch`, `GearNode`, and
  `ResolutionTrace` without leaking model implementation classes into the
  protocol schema?
- Which machine-readable combinations of the existing archive and Fact-filter
  criteria belong in the first MCP search tool?
- Should the Spring composition class live directly in
  `retro-crawler-mycollection`, or should Spring integration eventually be an
  optional companion artifact if the collection module needs to stay entirely
  framework-neutral?
- Which authentication mechanism and reverse-proxy arrangement will be used on
  the QNAP deployment?
- Which search fields and matching rules constitute the useful first version
  of `search_gear`?

## Progress

- [x] Selected MCP and Streamable HTTP as the agent-facing interface.
- [x] Established the core, MCP adapter, generic server, and collection
  composition boundaries.
- [x] Established the required `RetroCrawler` bean contract.
- [x] Defined the initial MCP tool surface and crawl lifecycle.
- [x] Added the shared asynchronous crawl-operation service with single-crawl
  admission, progress, cancellation, failure status, and bounded history.
- [x] Added optional cron-and-zone scheduling of full crawls through that same
  operation service.
- [x] Reused the Stash, Query, Batch, crawl metadata, and ResolutionTrace
  contracts delivered by issue #22.
- [ ] Define the bounded MCP projections of those core contracts.
- [x] Add `retro-crawler-mcp` and its auto-configuration tests.
- [x] Add the generic `retro-crawler-server` host and startup-contract tests.
- [x] Add collection-specific `Model` publication and server-owned location
  binding and crawler composition.
- [ ] Add authentication and authorization.
- [ ] Add Docker runtime composition and NAS deployment configuration.
- [ ] Verify MCP interoperability with Codex and at least one other client.

## Verification

- Applied the canonical formatter to all new Java sources and tests.
- Focused core, MCP auto-configuration, and server composition tests cover the
  shared operation service, successful completion, admission conflicts,
  cooperative cancellation, failure reporting, and bounded history.
- Scheduler tests cover disabled-by-default composition, cron and zone
  registration, full-crawl submission, and skipping an overlapping trigger.
- Rebased issue #37 onto `origin/main` at `7f0a446`, including the merged issue
  #22 Stash/query/provenance groundwork, and reconciled the Locations builder
  test with the current `RetroCrawler.crawl(...)` API.
- `mvn test` passed for the complete nine-module reactor after the rebase.
- `mvn clean install` passed for the complete nine-module reactor after adding
  the crawl-operation service.
- The packaged server loaded the external MyCollection and model JARs through
  `PropertiesLauncher`, initialized an MCP Streamable HTTP session, listed and
  called `list_archives`, and shut down gracefully with the service bean.
- With a temporary once-per-second cron expression, the packaged server
  discovered the scheduling configuration, started asynchronous full crawls,
  and stowed both configured archives through the external collection.
- Generated Spring configuration metadata for the server's location and
  operational property beans.
- Confirmed that the server artifact is an executable Spring Boot JAR with
  `RetroCrawlerServerApplication` as its start class.
- Confirmed that the packaged server contains `retro-crawler-mcp` and does not
  contain `retro-crawler-mycollection`.
- Confirmed that the executable uses `PropertiesLauncher` and starts with
  `retro-crawler-mycollection` plus its runtime dependencies supplied
  externally.
- Repeated the MCP Streamable HTTP handshake against the rebased packaged
  server, listed the registered tool, and called `list_archives`; the structured
  result contained the configured `hardware` and `software` archives.
