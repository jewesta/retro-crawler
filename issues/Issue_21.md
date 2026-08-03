# Issue 21: Add a Unified Builder for Annotation-Based and Manual Configuration

## Intent

RetroCrawler is intended to be a configurable toolset. The long-term concept
allows applications to configure a model in two complementary ways:

1. Declaratively, using RetroCrawler annotations.
2. Programmatically, using an elegant builder and manually registered
   implementations.

Annotation configuration must be parsed directly by RetroCrawler. It must not
depend on Spring beans, Spring component scanning, CDI, or another dependency
injection framework.

The initial implementation deliberately supports annotation-driven model
construction only. `Model.from(basePackage)` is the primary public model
construction path: the application names a package and RetroCrawler discovers
and interprets its annotated types. Explicit `Set<Class<?>>` and `TypeSource`
inputs remain available as deterministic escape hatches. This lets the
established annotation model remain intact while the `Model`,
type-discovery, and crawler-composition boundaries are introduced.

If programmatic model construction is added later, it should produce the same
`Model` rather than introduce a parallel runtime assembly path. Its detailed API
must be driven by concrete use cases rather than assumptions made during the
annotation-focused implementation.

## Public API Goal

`Model.from(...)` and `RetroCrawler.builder()` should form the primary,
discoverable composition API for RetroCrawler.

Conceptually:

```java
RetroCrawler crawler = RetroCrawler.builder()
		.model(Model.from("com.example.collection"))
		.repository(new JsonFileRepository(cacheDirectory))
		.build();
```

This is the API shape targeted by the initial implementation.

The repository is required configuration. The builder must not silently create
a `JsonFileRepository` or select any other repository implementation. Choosing
`new JsonFileRepository()` is still a concise option, but it is a choice made
explicitly by the application.

Manual model construction remains a valid future direction, but it is not part
of this first implementation.

## Working Builder Sketch

The public composition API has two distinct parts:

- `Model` describes a collection and how its archive artifacts are interpreted
  as gear.
- `RetroCrawler.Builder` composes that model with application infrastructure.

The common annotation-driven path is concise:

```java
Model model = Model.from("com.example.collection");

RetroCrawler crawler = RetroCrawler.builder()
		.model(model)
		.repository(new JsonFileRepository(cacheDirectory))
		.build();
```

The package overload discovers annotated types recursively below the base
package, then delegates to the same annotation parser used by the explicit
overloads:

```java
public final class Model {

	public static Model from(String basePackage) {
		// Discover annotated types, then delegate to from(types).
	}

	public static Model from(Set<Class<?>> types) {
		// Parse annotations and create the immutable model.
	}

	public static Model from(TypeSource source) {
		return from(source.types());
	}
}
```

`Model.from(String)` is the default mode of operation. `Model.from(Set)` is
useful for tests, constrained runtimes, generated indexes, and applications that
already have their own discovery mechanism. `Model.from(TypeSource)` preserves
the existing extension point for custom discovery.

The crawler builder consequently stays small:

```java
public interface RetroCrawler {

	static Builder builder() {
		// ...
	}

	interface Builder {

		Builder model(Model model);

		Builder repository(Repository repository);

		RetroCrawler build();
	}
}
```

Both `Model` and `Repository` are required. Neither has a fallback. The
repository is intentionally not part of the model or annotation configuration:
storage is an application composition concern, not a property of the collection
or gear declarations.

The unprefixed name `Model` is deliberate and consistent with other public
domain types such as `Repository`, `Gear`, `Archive`, and `Artifact`. Java
packages provide their namespace; branding every abstraction with `Retro` would
make the API unnecessarily repetitive.

### Automatic Type Discovery

Java does not offer a standard API for enumerating all classes below a package.
A robust implementation must account for classpath directories, ordinary and
nested JARs, context and custom class loaders, and the module path. RetroCrawler
should not implement those mechanisms from scratch.

ClassGraph 4.8.184 is the built-in discovery implementation. It has no runtime
dependencies, scans both the classpath and JPMS module path, supports nested
JARs and custom class loaders/module layers, and can inspect annotation metadata
before loading candidate classes. Adding it to `retro-crawler-core` is a
deliberate exception to the preference for a small core dependency set because
automatic discovery is part of the intended primary experience, not an
optional integration.

The discovery contract should be narrow:

- Scan the named package and its subpackages.
- Discover types carrying `@RetroCollection` or `@RetroGear`.
- Inspect annotation metadata without initializing every class in the package.
- Load only candidate model types, using the scanner's corresponding class
  loader.
- Sort candidates by fully qualified class name before model parsing so
  discovery order is deterministic.
- Report an actionable error when the package is blank, no model annotations
  are found, a candidate cannot be loaded, or visibility prevents scanning.
- Never silently fall back to a partial model.

Classes referenced from annotations, such as matchers, parsers, and clue
finders, do not need separate discovery. They remain reachable from the
discovered archive and gear declarations.

Automatic scanning cannot promise visibility into every possible runtime.
Named modules must expose relevant packages, and some application servers or
plugin systems use custom loaders that need explicit handling. The explicit
`Set<Class<?>>` and `TypeSource` overloads remain the reliable fallback rather
than weakening error reporting in `Model.from(basePackage)`.

The implementation is verified against exploded Maven test classes, an ordinary
JAR reached through a custom context class loader, recursive subpackages,
missing model annotations, multiple/missing collection declarations, and candidate
class loading without static initialization. The current Vaadin module does not
produce a nested Spring Boot JAR, and the project does not contain JPMS modules,
so those two environments are not represented by project fixtures. ClassGraph
provides those runtime mechanisms; `Model.from(Set)` and
`Model.from(TypeSource)` remain the explicit fallback if a particular
deployment does not expose its model packages to scanning.

### Likely Internal Model

The immutable `Model` contains the annotation-derived domain configuration
needed to assemble a crawler:

- One collection definition.
- The configured clue-finder instances.
- Fact definitions keyed by clue/fact key.
- Gear specialists keyed by gear type.

The crawler builder combines this model with its repository, then creates
`ArchiveDigger`, fact finders, gear resolution, and `RetroCrawlerImpl`.
Annotation reflection and model validation belong in `Model.from(...)`.

The existing relationships between `GearSpecialist`, `GearMatcher`,
`GearFactory`, descriptors, and reflective injection remain unchanged in this
phase. They should not be generalized or made generic solely for a hypothetical
manual configuration API.

### Deferred Manual Model Construction

A future `Model.builder()` remains conceptually compatible with this design. It
could support manual and hybrid construction while still producing the same
immutable `Model`. That later design must decide, based on real use cases:

- Whether registrations operate on specialists or separate matcher and factory
  capabilities.
- Whether matcher, factory, context, and specialist types benefit from generic
  parameters.
- How annotation-derived entries can be supplemented or replaced.
- How completely annotation-free fact and gear definitions are expressed.

None of those decisions are required to establish `Model.from(...)` as the
default mode of operation.

## Implemented State

### Annotation-Based Configuration

The annotation-driven path is implemented through `Model` and works end to end.
`Model.from(String)` recursively discovers annotated types beneath an
application-supplied base package. `Model.from(Set<Class<?>>)` and
`Model.from(TypeSource)` feed the same deterministic annotation parser without
requiring classpath scanning.

The current annotations configure:

- `@RetroCollection`
  - Collection ID
  - Display name
  - Filesystem locations
  - Working directory
- `@RetroClues`
  - Folder-name clue finder
  - File-name clue finders
  - File-content clue finders
  - Folder-tree clue finders
- `@RetroFactParser`
  - Collection-specific overrides for a selected fact parser
- `@RetroGear`
  - Gear type
  - Gear matcher implementation
- `@RetroFact`
  - Clue/fact key
  - Optionality
  - Strict or lenient matching
  - Fact parser implementation or automatic parser selection
- `@RetroId`
  - Gear identity field
- `@RetroAnyAttribute`
  - Destination for unassigned clues and facts

Annotation parsing and object creation are implemented with ordinary Java
reflection:

- `Model` locates `@RetroCollection` and assembles the model-owned runtime
  components.
- `ArchiveDescriptor.of` reads collection source metadata.
- `ArchivePathClueFinder.of` reads clue-finder declarations and creates the
  declared implementations.
- `GearDescriptor.of` reads gear and field annotations.
- `GearResolverFactory` creates matchers, parsers, fact finders, and gear
  specialists.
- `Reflection.newInstance` constructs annotation-declared classes through public
  no-argument constructors.

`RetroCrawler.builder()` requires the model and repository, then assembles the
archive digger and crawler. It never selects a repository implicitly.

`RetroCrawlerFactory` remains temporarily available as a deprecated
compatibility façade. It now requires a `Repository` in its constructor and
delegates to `Model.from(types)` and the builder; its former no-argument,
default-repository constructor has been removed.

No Spring configuration mechanism is involved in `retro-crawler-core`.

### Existing Public Tools and Extension Points

The project already exposes many of the interfaces required by a manual
configuration API:

- `FolderNameClueFinder`
- `FileNameClueFinder`
- `FileContentClueFinder`
- `FactParser`
- `GearMatcher`
- `GearFactory`
- `GearTreeFactory`
- `Repository`

Some lower-level classes can already be constructed from instances. For
example, `ArchivePathClueFinder` accepts clue-finder instances directly.
`GearTreeFactory` is already a complete programmatic extension point supplied
for each crawl.

The initial implementation also contains a TODO in `GearResolverFactory` to
make the factory configurable so users can supply their own default parsers.
This is evidence of the intended manual configuration direction.

### Deferred Manual Model Construction

There is no manual model-construction path in this phase:

- No precedence or conflict rules exist for combining annotations with manual
  registrations.
- Clue finders, parsers, matchers, and gear factories cannot generally be
  registered as instances through the top-level public API.
- `GearDescriptor` can only be created from annotations.
- `FactDescriptor` requires a `RetroFact` annotation.
- `GearResolver` and its descriptors remain implementation details rather than
  public manual-registration surfaces.

This is intentional. Annotation references are the only end-to-end way to
connect implementations to a model until concrete manual-configuration use
cases justify `Model.builder()`.

## Architectural Direction

Introduce one shared configuration and assembly pipeline:

```text
Base package --> Type discovery --+
                                  |
Explicit types / TypeSource ------+--> Annotation parsing
                                              |
                                              v
                                       immutable Model ----+
                                                          |
Repository -----------------------------------------------+--> RetroCrawler.Builder
                                                                      |
                                                                      v
                                                                 RetroCrawler
```

The boundary distinguishes between:

- Annotation-derived domain configuration in `Model`.
- Required application composition, namely a `Model` and `Repository`, in
  `RetroCrawler.Builder`.
- Runtime inputs, such as `Monitor`, reindexing, and result factories, which
  should remain crawl-operation parameters.

Implementation and orchestration classes should not become public configuration
surfaces merely because the builder uses them internally.

## Future Combination and Override Semantics

If manual model construction is introduced later, combining it with annotation
configuration will require explicit rules for:

- Whether manual registrations replace or supplement annotation-derived
  registrations.
- How duplicate clue finders are handled.
- How a manually registered parser overrides an annotation-declared or
  automatically selected parser.
- How conflicting gear definitions or matchers are reported.
- Whether configuration order matters.
- When the combined configuration becomes immutable.

Prefer deterministic behavior and actionable validation errors over implicit
last-write-wins behavior. Issue 25 introduces the first deliberately narrow
`Model.builder()` slice: it selects the same annotation-derived types as
`Model.from(...)` while overriding runtime locations, working directory, and
standard fact-parser configuration. Annotation-free fact and Gear registration
remains deferred.

## Relationship to Issue 17

Issue 17 extracts persistence behind the `Repository` interface. Its interim
factory retained `JsonFileRepository` as a backward-compatible default.

Issue 21 provides the public composition model through which an application can
select that repository or supply another implementation. The responsibilities
remain separate:

- Issue 17 defines and completes the persistence extension point.
- Issue 21 separates annotation-derived model construction from application
  composition through the crawler builder.

Issue 21 deliberately does not carry the interim repository default into the
new composition API. `JsonFileRepository` remains a supplied implementation,
but applications must select it explicitly. The no-argument
`RetroCrawlerFactory` path must therefore be removed, deprecated, or otherwise
kept out of the new primary API so it cannot undermine this requirement.

The work for issues 17 and 21 is intended to be merged into `main` together.

## Implementation

- [x] Define the immutable public `Model`.
- [x] Implement `Model.from(String)` with recursive base-package discovery.
- [x] Preserve `Model.from(Set<Class<?>>)` and `Model.from(TypeSource)` as
      explicit alternatives using the same annotation parser.
- [x] Select and pin ClassGraph 4.8.184 for discovery.
- [x] Verify discovery from exploded classes and an ordinary JAR supplied by a
      custom context class loader.
- [x] Introduce `RetroCrawler.Builder` with required `model(...)` and
      `repository(...)` entries.
- [x] Require explicit repository selection; do not provide a default.
- [x] Preserve external type discovery through `Set<Class<?>>` and `TypeSource`.
- [x] Integrate the pluggable `Repository` from issue 17.
- [x] Update current callers to use `Model.from(...)` and
      `RetroCrawler.builder()`.
- [x] Add focused tests for package discovery, explicit types,
      annotation-derived models, required crawler composition, and validation.
- [x] Document `Model.from(...)` and `RetroCrawler.builder()` as the primary
      composition API.
- [x] Add `Model.builder()` for annotation-derived models with runtime
      collection locations, working directory, and fact-parser overrides.

## Decisions

- `RetroCrawlerFactory` is deprecated for removal. Its remaining constructor
  requires an explicit repository.
- `Model` owns the annotation-derived `ArchiveDescriptor`,
  `ArchivePathClueFinder`, and `GearResolver`. The builder owns composition with
  the repository and creates `ArchiveDigger` and `RetroCrawlerImpl`.
- Fully manual fact, clue-finder, matcher, and Gear registration remains
  deferred; the current `Model.builder()` configures annotation-derived models.

## Verification

- `mvn -pl retro-crawler-core test`
- `mvn test`
- `mvn clean install`

The final core suite contains focused tests for package and subpackage
discovery, ordinary-JAR/custom-class-loader discovery, explicit type sets,
`TypeSource`, non-initializing candidate loading, model validation, required
builder entries, duplicate builder entries, and repository propagation.

## Out of Scope

- Spring bean configuration or Spring component scanning in
  `retro-crawler-core`.
- Requiring an external dependency injection container.
- Adding H2 or another database implementation.
- Configuring per-crawl runtime behavior during crawler construction.
- Annotation-free manual model registrations.
- Refactoring working matcher, factory, context, or specialist contracts for
  hypothetical manual configuration.
