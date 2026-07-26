# Issue 21: Add a Unified Builder for Annotation-Based and Manual Configuration

## Intent

RetroCrawler is intended to be a configurable toolset. Applications should be
able to configure it in two complementary ways:

1. Declaratively, using RetroCrawler annotations.
2. Programmatically, using an elegant builder and manually registered
   implementations.

Annotation configuration must be parsed directly by RetroCrawler. It must not
depend on Spring beans, Spring component scanning, CDI, or another dependency
injection framework.

The annotation-driven and programmatic approaches should not become two
independent construction pipelines. An annotation configurer should inspect the
provided types and contribute configuration through the same builder and
registration model that is available to application code.

Applications should be able to combine both approaches. Annotation-derived
defaults may configure most of a crawler while explicit builder registrations
provide or replace selected tools.

## Public API Goal

The builder should be the primary, discoverable composition API for
RetroCrawler. Public interfaces should represent the tools that applications
may implement and register.

Conceptually:

```java
RetroCrawler crawler = RetroCrawler.builder()
		.configure(Annotations.from(types))
		.repository(repository)
		.register(clueFinder)
		.register(factParser)
		.register(gearMatcher)
		.build();
```

This example illustrates the intended shape, not a finalized API. Method names,
registration granularity, and override rules remain to be designed.

The builder should accept actual implementation instances, and possibly
factories or suppliers where useful. Manual configuration must not impose the
public no-argument-constructor restriction currently required for
annotation-declared implementation classes.

## Current State

### Annotation-Based Configuration

The annotation-driven path is substantially implemented and works end to end.
`RetroCrawlerFactory.reflectOn` accepts a caller-supplied set of types and
manually parses their annotations.

There is deliberately no core classpath scanner. Applications may discover
types themselves and provide either a `Set<Class<?>>` or a `TypeSource`.

The existing annotations configure:

- `@RetroArchive`
  - Archive ID
  - Display name
  - Filesystem locations
  - Path-name clue finder
  - File-name clue finders
  - File-content clue finders
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

- `RetroCrawlerFactory` locates `@RetroArchive` and assembles the major runtime
  components.
- `ArchiveDescriptor.of` reads archive metadata.
- `ArchivePathClueFinder.of` reads clue-finder declarations and creates the
  declared implementations.
- `GearDescriptor.of` reads gear and field annotations.
- `GearResolverFactory` creates matchers, parsers, fact finders, and gear
  specialists.
- `Reflection.newInstance` constructs annotation-declared classes through public
  no-argument constructors.

No Spring configuration mechanism is involved in `retro-crawler-core`.

### Existing Public Tools and Extension Points

The project already exposes many of the interfaces required by a manual
configuration API:

- `PathNameClueFinder`
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

### Missing Programmatic Composition

There is currently no complete builder-based configuration path:

- No `RetroCrawler` builder exists.
- No common configuration model or registry exists.
- No configurer interface exists.
- Annotation parsing constructs final runtime objects instead of contributing
  registrations to a builder.
- No precedence or conflict rules exist for combining annotations with manual
  registrations.
- Clue finders, parsers, matchers, and gear factories cannot generally be
  registered as instances through the top-level public API.
- `GearDescriptor` can only be created from annotations.
- `FactDescriptor` requires a `RetroFact` annotation.
- `GearResolver` and `RetroCrawlerImpl` cannot be assembled through the public
  API.
- `RetroCrawlerFactory` always follows the annotation-based path.

The result is that most extension interfaces exist, but annotation references
are currently the only end-to-end way to connect their implementations to a
crawler.

## Architectural Direction

Introduce one shared configuration and assembly pipeline:

```text
Annotated types
      |
      v
Annotation configurer ----+
                          |
Manual registrations -----+--> Builder/configuration model
                                      |
                                      v
                              Validation and assembly
                                      |
                                      v
                                RetroCrawler
```

The annotation configurer should translate annotations into the same kinds of
registrations offered by the builder. Validation should run on the combined
configuration rather than being duplicated between configuration styles.

The builder should distinguish between:

- Required domain configuration, such as the archive and supported gear.
- Optional tools with useful defaults.
- Runtime inputs, such as `Monitor`, reindexing, and result factories, which
  should remain crawl-operation parameters.

Implementation and orchestration classes should not become public configuration
surfaces merely because the builder uses them internally.

## Combination and Override Semantics

The two configuration styles must be usable together. This requires explicit
rules for:

- Whether manual registrations replace or supplement annotation-derived
  registrations.
- How duplicate clue finders are handled.
- How a manually registered parser overrides an annotation-declared or
  automatically selected parser.
- How conflicting gear definitions or matchers are reported.
- Whether configuration order matters.
- When the combined configuration becomes immutable.

Prefer deterministic behavior and actionable validation errors over implicit
last-write-wins behavior.

## Relationship to Issue 17

Issue 17 extracts persistence behind the `Repository` interface while retaining
`JsonFileRepository` as the default.

Issue 21 provides the public composition model through which an application can
select that repository or supply another implementation. The responsibilities
remain separate:

- Issue 17 defines and completes the persistence extension point.
- Issue 21 exposes extension points coherently through annotation and builder
  configuration.

The work for issues 17 and 21 is intended to be merged into `main` together.

## Implementation Outline

- [ ] Define the shared configuration model used during crawler assembly.
- [ ] Introduce the public builder entry point.
- [ ] Define a configurer contract.
- [ ] Extract annotation parsing into an annotation configurer.
- [ ] Make the annotation configurer populate the shared configuration model.
- [ ] Add manual registration methods for the supported public tools.
- [ ] Define combination, override, and duplicate-registration semantics.
- [ ] Preserve the current annotation-only construction path as a concise
      convenience.
- [ ] Preserve external type discovery through `Set<Class<?>>` and `TypeSource`.
- [ ] Allow manually supplied instances to use constructor dependencies.
- [ ] Integrate the pluggable `Repository` from issue 17.
- [ ] Add focused tests for annotation-only, builder-only, and hybrid
      configuration.
- [ ] Document the builder as the primary composition API and annotations as a
      first-party configurer.

## Open Design Questions

- Does builder-only configuration need to support completely annotation-free
  gear models, including field/fact mapping, or does it initially register
  runtime tools around annotated gear classes?
- What is the smallest useful registration unit for gear construction:
  descriptors, matchers and factories, or a higher-level gear definition?
- Should configurers mutate a builder, contribute to a separate configuration
  object, or return immutable configuration fragments?
- Should manual registrations always take precedence over annotation-derived
  registrations, or should replacement be explicit?
- Should `RetroCrawlerFactory` remain a supported lower-level public API after
  the builder becomes the preferred entry point?
- Which implementation types should accept instances, suppliers, or both?

## Out of Scope

- Spring bean configuration or Spring component scanning in
  `retro-crawler-core`.
- Requiring an external dependency injection container.
- Implementing package scanning in the core.
- Adding H2 or another database implementation.
- Configuring per-crawl runtime behavior during crawler construction.

