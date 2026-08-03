# RetroCrawler Agent Guidelines

These instructions apply to the entire repository.

## Design Principles

1. **Prefer real-world archive language.**
   RetroCrawler models the behavior of a physical archive, and its vocabulary is
   intentional. Its playful, informal tone is part of the project's identity;
   names such as `RetroCrawler` and `Gear` should not be sanitized into sterile
   technical vocabulary. Prefer terms from archives, crawling, clues, artifacts,
   and gear over generic software or CRUD terminology when the domain offers a
   suitable word. In particular, a `Repository` uses `retrieve` to obtain an
   archive and `stowaway` to put one away. Do not rename these operations to
   `load`, `save`, `read`, or `persist` merely to follow common framework
   conventions.

2. **Keep collectors and their gear at the center.**
   RetroCrawler primarily serves collections of retro material, especially
   hardware, while remaining useful for other kinds of collectors' archives.
   `Gear` is the deliberate umbrella abstraction for an item in a collection,
   regardless of whether that item is hardware or something else. Preserve this
   domain focus: do not narrow `Gear` to a particular kind of retro hardware or
   dilute it into generic terms such as `entity` or `item` merely to make the
   framework sound more universal.

3. **Keep the core a small, reusable framework.**
   `retro-crawler-core` must not depend on the demo application, CLI, or Vaadin
   application. It must likewise remain independent of the optional shared
   model. Avoid adding heavy dependencies to the core when an extension point
   or an optional module would suffice.

4. **Keep shared model vocabulary objective and portable.**
   `retro-crawler-model` contains value types and canonical parsers whose
   meaning is stable beyond one collector's archive. This includes formal
   standards, industry vocabulary, and durable community reference systems
   such as The Retro Web. It must not contain personal cataloguing policy,
   collection-specific clue syntax, concrete fallback gear, or subjective
   assessments. Collection adapters decide how their clues map onto the shared
   vocabulary.

5. **Treat the filesystem archive as the source of truth.**
   A repository stores the extracted clue archive so that expensive crawling can
   be avoided. Stored data is rebuildable from the configured archive locations.
   Keep repository abstractions independent of a particular storage technology.

6. **Preserve useful defaults while allowing explicit configuration.**
   Existing callers should continue to work with sensible default behavior.
   New extension points should also be injectable and configurable without
   requiring application-specific framework integration.

7. **Make collections explorable by humans, applications, and AI agents.**
   RetroCrawler is a local, deterministic collection toolkit. Its core should
   expose structured, machine-readable queries and traceable results rather
   than depend on an LLM, an AI SDK, or natural-language prompting. Keep AI and
   protocol integrations in thin adapters that use the same public API as the
   CLI, Vaadin sample application, and other consumers. Preserve provenance
   through the resolution process so an answer about a piece of gear can be
   traced back to its archive, source path, clues, and facts.

8. **Keep clues and facts on separate levels.**
   A `Clue` is model-independent evidence observed while crawling an archive;
   an `Artifact` and its repository representation contain clues, never facts.
   A `Fact` is a model-dependent, typed interpretation produced during gear
   resolution and must retain its source clue. Clue finders may preserve keys
   explicitly present in their source format, but must not consult the model's
   known fact-key registry. Resolution may derive an effective clue view, but
   must not mutate the cached artifact. Model and parser changes must therefore
   be able to reinterpret an existing clue archive without re-indexing.

## Repository Structure

- `retro-crawler-core`: public framework API and implementation.
- `retro-crawler-model`: optional shared facts and canonical parsers.
- `retro-crawler-demo`: sample archive types, clue finders, and demo data.
- `retro-crawler-app`: Vaadin demonstration application.
- `retro-crawler-cli`: command-line demonstration application.
- `retro-crawler-tools`: project-specific documentation and build plumbing.
- `retro-crawler-tools/documentation`: documentation support and examples.

The application and CLI are examples rather than stable public APIs. The Vaadin
application remains a valuable visual browser, demonstration, and integration
check, but it is not the primary product boundary. Future AI or protocol
adapters should likewise remain outside `retro-crawler-core`.

## Issue Documentation

- Keep living issue notes in the repository-root `issues` directory.
- Use one Markdown file per issue, named exactly `Issue_<issue_number>.md`.
- Record the issue's intent, domain modelling, decisions, progress, open
  questions, and verification where relevant.
- Update the issue note as work progresses; it is a working design record, not
  only a summary written after implementation.

## Build and Verification

- The project targets Java 21 and is built as a Maven multi-module reactor.
- The canonical formatter is `prettify` from the separately checked-out
  `jewesta/devtools` repository. Its `prettify/formatting-rules.xml` profile can
  also be imported into Eclipse or STS.
- Use `run/prettify.sh --apply <java-file>...` (or the matching `.bat` launcher)
  to clean up and format concrete Java files. Use `--assert` for a check-only
  run; omitting file paths selects all tracked Java sources. The launcher finds
  devtools through `WESTARPS_DEVTOOLS_HOME`, local Git configuration
  `westarps.devtools.path`, or the conventional sibling `../devtools` checkout.
- Run `mvn test` from the repository root for the normal test suite.
- Run `mvn clean install` when changes must be verified across packaged module
  boundaries.
- Add focused tests for changed core behavior, especially extension-point
  contracts and persistence behavior.
- Core code uses SLF4J. Do not introduce `System.out`, `System.err`, or
  `java.util.logging` into `retro-crawler-core`.

## Change Discipline

- Preserve unrelated user changes in the working tree.
- Keep public API naming consistent with the domain vocabulary above.
- Use component-style accessors for immutable framework state and
  configuration, with `is...` or `has...` reserved for predicates. Do not add
  zero-argument JavaBeans-style getters to `retro-crawler-core` or immutable
  shared-model values; map-like keyed `get(key)` operations remain ordinary
  lookups.
- Prefer small, reviewable changes and avoid unrelated refactoring in issue
  branches.
