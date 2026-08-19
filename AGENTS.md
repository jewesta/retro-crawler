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
   traced back to its archive, source ARIs, clues, and facts.

8. **Keep clues and facts on separate levels.**
   A `Clue` is model-independent evidence observed while crawling an archive;
   an `Artifact` and its repository representation contain clues, never facts.
   A `Fact` is a model-dependent, typed interpretation produced during gear
   resolution and must retain its source clue. Clue finders may preserve keys
   explicitly present in their source format, but must not consult the model's
   known fact-key registry. Resolution may derive an effective clue view, but
   must not mutate the cached artifact. Model and parser changes must therefore
   be able to reinterpret an existing clue archive without re-indexing.

9. **Give every clue key exactly one authority per artifact.**
   A single clue may deliberately contain several values, but two distinct
   clues must never claim the same key within one artifact. Different finders
   are independent sources, not corroborating authorities: a folder name and a
   metadata file supplying the same key is an archive consistency error even
   when their values agree. Never merge their values or let source order choose
   a winner. Any number of clue finders may emit anonymous clues: anonymous
   clues claim no semantic key, coexist under distinct generated keys, and may
   later contribute to the same fact. During resolution, an anonymous
   observation must not compete with a clue that already explicitly claims its
   semantic key. Generated anonymous-key collisions are the sole key-level
   exception; re-key the incoming anonymous clue because its key carries no
   semantics.
   The rule lives in the type. `Clues` is the currency between a clue finder,
   the crawler, and an `Artifact`: immutable, in observation order, one clue per
   key, and constructible only through a `ClueAccumulator`. That accumulator is
   the single place a clue is ever inspected — once, as it arrives, against
   everything observed so far. Do not re-validate `Clues` that arrive from
   somewhere else, and do not reintroduce a `Set<Clue>`: `Clue` keeps identity
   equality on purpose, so a set promises a uniqueness it cannot enforce and
   says nothing about the key-level rule that actually applies.
   Because the rule rejects rather than merges, it must say where. Report a
   clue failure against the authoritative ARI that caused it. The digger
   attaches the running finder's unique simple class name and defaults the
   failure source to the candidate folder; `ArchiveFileView.peek(...)` replaces
   that default with the exact file ARI when the failure occurs while inspecting
   content. A finder that already tracks offsets should pass a `ClueLocation`
   when it accumulates a clue, so a rejection can point at the tag the
   cataloguer actually wrote. `Clues` carries those positions so they survive a
   finder handing its work back, and `Artifact` drops them, because a cached
   archive has no active source text to point into. Never let a clue failure
   escape a crawl without an ARI.
   Finder identity and clue provenance are durable rather than diagnostic side
   channels. The crawler retains the producing finder's unique simple class
   name on every returned clue. A finder may explicitly declare zero, one, or
   several contributing resource ARIs per clue; accessing, listing, or peeking
   at a resource never implies that it caused a clue. Folder and file views
   carry authoritative ARIs and provide convenient sourced clue factories.
   Every declared source must be an exact member of the pruned folder view the
   finder received. Being path-wise below the candidate is insufficient because
   a child artifact is deliberately absent from that view. Validate a finder's
   complete returned `Clues` before accumulating any of it, and reject the
   complete result when one source falls outside the readable boundary.
   Persist finder names and explicitly declared sources through `Artifact` and
   into the source `Clue` retained by a resolved `Fact`; omit absent sources
   rather than inventing them.
   Addressable archive resources remain ARIs after resolution. `ParseContext`
   carries the current artifact ARI, and the default `ARIParser` resolves raw
   artifact-relative references below it. Never inject a provider `Path` into
   Gear merely to convert it back into an ARI at consumption time; provider
   paths are internal crawl coordinates.

10. **Express gear relation through archive location, never through
   hierarchy-derived type.**
   A physical item is in exactly one place at a time, and so is a folder. That
   isomorphism is why RetroCrawler models gear relation as archive location:
   moving gear means moving folders, which is fast, pragmatic, and visual, with
   no parallel relational model to keep in sync and no way for a relation to
   contradict the archive. Do not add relation types, cross-references, or link
   tables beside the tree.
   The same exclusivity forbids the inverse. A folder's ancestry must never
   determine what its gear *is*: `Graphics Cards/GeForce 2` does not make the
   GeForce 2 a graphics card, because moving that folder would silently change
   the item's type. Clue finders may read a folder's own name; they must not
   read classification from its parents. A `ClueFinder` may descend through
   non-gear subfolders belonging to one item, but folders that already
   established an artifact are pruned from its view and must stay pruned. The
   tree carries where a thing is, never what a thing is.
   Metadata-folder status is established, never inferred. Only a folder the
   crawl positively read and found no clue in is one, and only such a folder may
   be read through by an ancestor's finder; a folder in any other state,
   including one the crawl never determined, stays opaque. Keep this structural:
   a `FolderOutcome` carries the readable view only when it has established the
   right to offer one, so a folder holding another item's evidence has nothing
   to hand up and the boundary cannot be crossed by a forgotten check. The type
   is sealed, so a new outcome stops every switch over it compiling until it
   declares what it offers.

## Repository Structure

- `retro-crawler-core`: public framework API and implementation.
- `retro-crawler-model`: optional shared facts and canonical parsers.
- `retro-crawler-demo`: sample archive types, clue finders, and demo data.
- `retro-crawler-app`: Vaadin demonstration application.
- `retro-crawler-cli`: command-line demonstration application.

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

## Issue Worktrees

- Implement each issue in a dedicated sibling Git worktree unless the user
  explicitly requests a different workflow.
- Name the worktree directory `<repository-name>-issue-<issue_number>`, for
  example `retro-crawler-issue-28`.
- Create the worktree branch as `issues/Issue_<issue_number>` from `main`.
- Keep the primary repository checkout on its existing branch; do not switch it
  to the issue branch merely to create the worktree.

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
