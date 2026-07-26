# Issue 24: Prove RetroCrawler Against a Real-World Collection Model

## Context

RetroCrawler has so far been developed primarily against a deliberately small
demo collection. The framework concepts are plausible, but they have not yet
been tested against a large, human-maintained archive whose directory structure
and naming conventions evolved organically over many years.

The real collection contains vintage computers, computer components, video game
consoles, games, software, photographs, manuals, drivers, disk images, purchase
records, repair histories, and other supporting material. Its filesystem is the
source of truth.

The collection broadly has two directory roots:

- A heterogeneous manufacturer-oriented archive containing consoles, games,
  computers, and unrelated technical or household equipment.
- A more focused archive for IBM-compatible computers, complete systems,
  components, and software.

The actual paths and crawled data are private and must remain local.

## Intent

Create an in-repository collection-model module that describes this collection
using the same public RetroCrawler APIs available to any other consumer.

The model is both useful software for the collection and a real-world
conformance test for the framework. Friction discovered while implementing it
should drive small, general framework improvements rather than collection-
specific behavior in `retro-crawler-core`.

The model must also make unfinished cataloguing work visible. RetroCrawler
should help answer not only "what gear is present?" but also "what still needs
an ID, better tags, corrected data, or improved model support?"

The intended namespace is:

```text
com.retrocrawler.mycollection
```

The likely Maven module is:

```text
retro-crawler-mycollection
```

## Prime Directive: Preserve the Framework Boundary

The personal collection model must not become a privileged code path.

- `retro-crawler-mycollection` depends on `retro-crawler-core`.
- `retro-crawler-core` must not depend on or reference the personal model.
- The model uses only public framework APIs.
- Collection-specific tag names, hardware taxonomies, aliases, parsers, and
  matchers remain outside the core.
- A core change is justified only when it expresses a capability useful to an
  arbitrary collection model.
- The demo remains small and instructional. It should adopt reduced examples of
  generalized behavior rather than grow into a sanitized copy of the personal
  model.

The model may live in the same Maven reactor without weakening this boundary.
Being a first-party consumer makes it a useful integration check while its
dependency direction keeps it honest.

## Privacy and Data Boundary

The model code and the collection data have different privacy characteristics.
The ordering scheme and vocabulary can live in the repository; the crawled
material cannot.

The following rules apply:

- Do not commit real archive paths.
- Configure source roots and repository/cache locations at runtime.
- Do not copy real filenames, serial numbers, personal names, notes, images,
  manuals, drivers, or other collection contents into test resources.
- Use invented or anonymized filesystem fixtures in automated tests.
- Normal Maven tests must never require mounted personal volumes.
- Any live-archive integration check must be explicit, local, and excluded from
  the default reactor build.
- Generated clue archives may contain private filenames and Markdown notes.
  They must be stored outside the repository and treated as private data.
- File-content inspection should be opt-in and restricted to specifically
  configured files such as `retro.md`.

## Initial Read-Only Reconnaissance

On 2026-07-26, both collection roots were inspected read-only for structural
orientation.

The inspection:

- Enumerated directory and file names, nesting, extensions, counts, and file
  metadata.
- Did not read file contents.
- Did not follow symbolic links.
- Did not modify the collection.
- Did not write collection data into the repository.

The results are a point-in-time snapshot of an evolving archive.

### Scale

| Archive area | Descendant directories | Files | Maximum observed depth |
|---|---:|---:|---:|
| IBM-compatible archive | 6,364 | 44,432 | 12 |
| Manufacturer archive | 3,631 | 38,191 | 10 |
| Combined | 9,995 | 82,623 | — |

The IBM archive is distributed approximately as follows:

| Top-level area | Descendant directories |
|---|---:|
| Components | 5,210 |
| Software | 662 |
| Complete systems | 474 |
| Other top-level areas | 18 |

Photographs and PDFs account for a large part of the IBM archive, alongside
disk images, compressed archives, drivers, executables, web references, and
other historical software. The manufacturer archive likewise contains many
photographs and documents, but also large ROM and software archives.

This scale confirms that the extracted, cached clue archive is an essential
part of the design. Interactive consumers should not repeatedly traverse the
live source filesystem.

## Observed Collection Semantics

### One Gear, One Folder

The collection generally follows the rule:

> One piece of gear is represented by one folder.

This is a strong fit for RetroCrawler's existing folder-oriented artifact
boundary.

However, not every folder is gear. Gear folders coexist with:

- Taxonomy and grouping folders.
- Documentation and manuals.
- Drivers, BIOS files, disk images, and software.
- Acquisition and seller records.
- Repair and restoration events.
- Photographs of the complete system.
- Backups and installation media.
- Temporary, experimental, and research material.

Recognition therefore cannot treat every traversed directory as gear.

### Gear Can Contain Gear

When a computer is assembled, component gear folders are moved inside a
complete-system folder. Components can themselves contain other gear, such as
processors, memory, accessories, manuals, or original media.

Filesystem nesting consequently represents meaningful physical or archival
containment. It must remain distinct from classification:

- A graphics card inside a computer build is still a graphics card.
- Its parent relationship says where it currently belongs.
- Its gear type says what it is.

The compressed tree emitted by the current crawler is a promising foundation:
unresolved category and support folders can be lifted away while resolved gear
remains attached to the nearest resolved gear ancestor.

The hierarchy is deliberately mutable. It describes the current location and
containment of gear, not historical identity or a type that must be preserved.
Moving a graphics card into a computer build removes its old taxonomy parents.
After rebuilding the disposable archive, the card must be classified from the
facts that remain on its own folder. If those facts are insufficient, resolving
it as `MysteryGear` is correct.

RetroCrawler must not preserve an old classification secretly or write it back
to the source tree.

### Ignoring Is Deliberate Behavior

The manufacturer archive intentionally contains unrelated technical and
household material. Much of it is untagged. RetroCrawler must be able to ignore
such material without imposing a rigid, database-like filesystem layout.

At the same time:

- Some untagged folders are genuine gear.
- Untagged category folders can carry important context.
- Tagged folders can be manuals, packaging, software, or other artifacts rather
  than hardware gear.

The collection rule is:

> No tag, no gear.

A folder from which the configured clue finders obtain no tag clues remains
undiscovered. RetroCrawler should not guess that it represents gear from its
name or location alone. Such objects will enter the model over time as the
physical collection is swept and tags are added.

A valid `2xx.xxx` identifier is the strongest positive declaration that a
tagged folder represents a physical collection object. Tagged gear without such
an ID reflects cataloguing work that has not yet caught up with the physical
collection.

## The Folder-Name Language

Square-bracket groups form a small, human-evolved language rather than a fixed
tag enum.

### IBM-Compatible Archive

- 2,181 tagged directories.
- 5,505 bracket groups.
- Between one and seven groups on an individual tagged directory.
- 1,623 six-digit collection-ID occurrences.
- 859 serial-number-shaped groups.
- 243 `trw`-shaped groups.
- Additional recurring concepts include MAC addresses, chips, set composition,
  buses, memory sizes, speeds, form factors, dates, languages, packaging,
  lifecycle state, provenance, and condition.
- 163 empty groups used as incomplete placeholders.

### Manufacturer Archive

- 1,141 tagged directories.
- 2,589 bracket groups.
- 786 six-digit collection-ID occurrences.
- Frequent vocabulary for platform, region, language, media, packaging,
  completeness, edition, and lifecycle.
- 270 empty placeholder groups.

### Parsing Implications

The syntax includes:

- Keyed groups such as serial number, collection-reference, MAC address, chip,
  and set declarations.
- Anonymous values such as `ISA`, `PCI`, memory sizes, languages, colors, or
  lifecycle states.
- Comma-separated value lists.
- Decimal-comma values which must not be split as lists.
- Empty placeholders.
- Unknown and misspelled vocabulary.
- Case and formatting variations.
- Repeated or contradictory information.

Unknown values must remain available as clues, and incomplete information may
leave gear generic. That tolerance does not apply to violations of declared
collection invariants. Obvious inconsistencies must be reported as errors and
fixed in the source archive rather than skipped, merged, or guessed around.

### Title Extraction

The demo `SquareBracketsClueFinder` assumes that the title is everything before
the first bracket group. Real names disprove that assumption:

- 400 IBM directories begin with a bracket group.
- 402 contain ordinary text after the first bracket group.

Lifecycle state, ownership, provenance, or condition commonly precedes the
actual title. Other names interleave free text and bracket groups.

A real parser should:

- Preserve the original folder name.
- Preserve the raw bracket observations for traceability.
- Derive a usable title from all non-bracket text, not only the prefix.
- Parse known groups progressively without discarding unknown groups.

Whether raw group ordering needs an explicit representation remains an open
question, but parsing must not make the original observation unrecoverable.

## Hierarchy Is Snapshot Context, Not Type Evidence

The reconnaissance initially suggested inheriting classification facts from
ancestor directories because many gear folders omit locally repeated
information.

The graphics-card area gives a measurable example:

| Bus category | ID-tagged card folders | Bus repeated locally | Bus known only from ancestor path |
|---|---:|---:|---:|
| AGP | 14 | 6 | 8 |
| EISA | 2 | 1 | 1 |
| ISA | 38 | 3 | 35 |
| PCI | 22 | 11 | 11 |
| VLB | 18 | 3 | 15 |

Most ISA and VLB cards cannot obtain their bus fact from the current folder
name alone.

This is incomplete local cataloguing, not a reason to make parent taxonomy a
durable type declaration. A graphics card moved into a computer build loses its
old `Grafikkarten`, `ISA`, or `PCI` ancestors. RetroCrawler must then rebuild
from the new filesystem state.

Gear types should be inferred by weighing facts on the gear folder itself. For
example:

- `AGP` alone is strong graphics-card evidence.
- `PCI` alone is ambiguous.
- `PCI` together with `VGA`, `DVI`, or `HDMI` is strong graphics-card evidence.
- `PCI` together with `RJ45` instead supports a network-card classification.

If the available facts do not support a specialized type, a physical object
with a valid collection ID remains `MysteryGear` until its tags or the model are
improved.

The repository already retains the archive-node tree, so current containment is
not lost. Structured path access may still be needed later for provenance,
navigation, file location, and deliberate subtree pruning, but automatic
ancestor-fact inheritance is not an Issue 24 prerequisite.

## Identifier Namespaces and Identity

The collection uses several deliberate identifier namespaces:

- `2xx.xxx` identifies a physical object in the collection. In folder names it
  is normally written as six digits without the display separator.
- `1xx.xxx` identifies a scan of a document or, more rarely, a document obtained
  from the internet instead of being scanned locally.
- `FD-*` identifies a floppy-disk image.

The term `Gear` remains intentionally broad for physical collection objects. A
graphics card, physical manual, storage medium, console, game, or stamp can all
be gear. Hardware type is a refinement of gear, not a prerequisite for
membership.

The 2-series identifier is called the **Retro ID**. It has a physical
counterpart: the number is applied to a piece of paper with a pagination stamp
and that tag accompanies the physical object. The same number is then placed in
brackets in the gear folder name. Consistency between the physical tag and the
filesystem declaration is a central collection invariant, even though
RetroCrawler can directly validate only the filesystem side.

The identifier families must not be parsed as one undifferentiated numeric ID.
A physical gear folder may also reference related scan or floppy-image IDs.
Those identifiers describe related archival material rather than additional
physical identity.

A valid 2-series ID establishes physical gear identity even when no specialized
gear type can be inferred. This suggests distinct model value types such as:

```text
RetroId
ScanId
FloppyImageId
```

The real data must nevertheless be validated against the intended invariants.

Within the IBM archive:

- 1,623 ID occurrences were observed.
- They represent 1,603 distinct values.
- 19 values occur more than once.
- One value occurs three times.

The duplicate paths appear to represent several different situations:

- Accidental reuse of an ID for unrelated gear.
- A child folder repeating its parent folder's identity.
- Multiple folders apparently describing the same physical gear.
- A component left represented in both a taxonomy and a computer build.

Repeated 2-series physical identities are errors to be fixed in the collection.
RetroCrawler must not silently merge, skip, renumber, or otherwise bypass them.
It should collect actionable provenance for the conflicting observations and
fail validation of the rebuilt snapshot.

Repeated references to scan or floppy-image identifiers may have different
semantics and must not be treated as duplicate physical gear without applying
their namespace rules.

The current synthetic artifact ID is derived from the relative path. Moving a
gear folder into a computer build therefore changes that ID. This is acceptable:
the repository is a disposable snapshot that can be discarded and rebuilt.

The model still needs to distinguish:

- Snapshot-local source/artifact occurrence identity.
- Stable declared physical-gear identity.
- Related digital-material identities.
- Resolved gear type.

Tagged gear without a declared Retro ID remains incomplete cataloguing. It may
still be recognized by strong facts or become `MysteryGear`, but RetroCrawler
must report the missing central identity rather than invent a durable
replacement.

## Garbage In, Garbage Out

RetroCrawler is not a data-cleaning system. It should make uncertainty visible,
but it must not normalize away contradictions in the source of truth.

Three cases must remain distinct:

1. **Unknown:** a tag or fact is not yet understood. Preserve it as a clue.
2. **Incomplete:** expected information such as a physical collection ID is
   missing. Resolve conservatively where possible and report the omission.
3. **Invalid:** declared invariants conflict, such as duplicate physical IDs or
   contradictory singular facts. Reject the rebuilt snapshot with actionable
   diagnostics so the source archive can be fixed.

A rebuild should gather enough validation errors to make correction practical.
It must not publish a partially repaired or silently incomplete replacement
cache. Repository replacement should occur only after successful extraction and
validation; any previous cache is merely the last known snapshot and must not be
presented as current without an explicit warning.

## Cataloguing Audit Is a Primary Outcome

Resolving typed gear is only one useful outcome of a crawl. RetroCrawler should
also produce a structured cataloguing audit that identifies work still required
in either the collection or its model.

The audit should distinguish findings such as:

- Tagged gear without a Retro ID.
- A malformed or duplicate Retro ID.
- Gear resolved as `MysteryGear`.
- Unknown tags that no fact parser currently understands.
- Parsed facts that no specialized gear matcher uses.
- Multiple specialized matchers with equal or otherwise ambiguous support.

RetroCrawler should not guess whether a mystery needs better tags, a new
`GearMatcher`, or a cleverer existing matcher. It must expose the observations
and resolution process so the collector can make that decision:

- Original clues.
- Successfully parsed facts and their confidence.
- Unparsed or unused clues.
- The resulting specialized type or `MysteryGear`.
- Missing identity, ambiguity, and validation findings.

This allows the collector to make the domain decision:

- Correct the archive.
- Add a missing tag.
- Implement a new gear type and matcher.
- Improve an existing parser or matcher.
- Deliberately leave the gear generic.

These findings must be structured data suitable for the later query API, not
ephemeral log text.

## Mystery Gear and the Fallback Role

`MyMysteryGear` in the demo already represents the intended fallback concept.
It is built using core's `AnyGearMatcher`, which returns weak confidence for any
artifact so a more specific matcher can take precedence.

The real model should use the shorter, playful name `MysteryGear`.

The fallback role should be formalized in core, but the concrete gear class
should remain model-owned. Core cannot sensibly provide the concrete
`MysteryGear` without prescribing a gear object shape and weakening the
framework's bring-your-own-type design.

A formal fallback should have explicit resolver semantics:

1. Resolve available clues into facts.
2. Consider specialized gear matchers.
3. Select a unique sufficiently supported specialized type when possible.
4. Otherwise instantiate the configured model-owned fallback type.

Treating fallback as a distinguished role is clearer than making it an ordinary
weak matcher. Core should enforce at most one fallback type and should define
how ambiguity reaches it.

Folder-name, file-name, and file-content clue finders are equal framework
extension points. A model may legitimately discover gear entirely from files;
the framework must not make folder tags privileged or mandatory.

The collection model's current "no tag, no gear" rule is expressed by its
configured clue finders returning no clues for an untagged folder. It does not
imply a framework restriction. If the model later treats a `retro.md` or
`retro.properties` file as an alternative intentional description of gear, a
file-content clue from that file can establish the containing folder as an
artifact in exactly the same way as a folder-name clue.

## Notes and Legacy Metadata

The initial reconnaissance sample found:

- Six `retro.properties` files.
- Four `retro.md` files.

Their contents were not read.

A later, comprehensive case-insensitive filename sweep found eleven
`retro.properties` files and six `retro.md` files. That later count supersedes
the initial sample count; no contents were read during either sweep.

The properties files are remnants of a short-lived authoring experiment.
Properties editors are not widely available or collector-friendly, so they
should not define the future workflow. A legacy clue finder may still import
them if that proves useful.

A `retro.md` file is the preferred direction for free-form notes:

- Markdown is human-editable with common tools.
- It can be read as an optional file-content clue.
- Its content can become a notes fact associated with the containing gear.
- It may either contribute to already-recognized gear or help establish the
  containing folder as gear, according to the collection model's clue finder
  and matcher design.
- Its contents remain private and may be present in the local extracted clue
  archive.

## Existing Framework Strengths

The reconnaissance supports several existing design choices:

- The filesystem archive is the source of truth.
- One folder as the normal artifact boundary matches the collection.
- Clues allow imperfect and unknown observations to survive.
- Fact parsers can progressively assign meaning to the folder language.
- Gear matchers allow model-specific classification.
- The compressed gear tree can represent nested gear while omitting
  navigational folders.
- The repository abstraction avoids repeated traversal of a large archive.
- The repository is disposable and can be rebuilt after filesystem changes.
- File-name and file-content clue finders can discover photographs, manuals,
  notes, and other supporting evidence.

## Framework Pressure Discovered

### 1. Runtime Archive Locations

`@RetroArchive.locations` currently embeds source paths in annotation metadata
and requires at least one location during model construction.

Personal archive paths are deployment configuration, not model vocabulary. The
framework needs an explicit runtime configuration mechanism while preserving
convenient annotation defaults for demos and existing callers.

The exact API remains to be designed. It should keep `Model` immutable and
should remain compatible with future multi-archive composition.

### 2. Structured Path Context

The current repository already retains the parent/child archive tree. Structured
relative-path context may be useful for provenance, navigation, file location,
and traversal policy, but parent facts must not be inherited automatically for
gear-type classification. This is no longer a prerequisite for the first model
slice.

### 3. Deliberate Ignore and Traversal Policy

The model needs to distinguish:

- Traverse for possible descendant gear.
- Inspect the current folder for clues.
- Ignore the current folder as gear while retaining it as context.
- Potentially skip an irrelevant subtree entirely.

This should be a general archive-crawling capability rather than a hardcoded
manufacturer or folder-name list.

### 4. Stable Identity and Conflict Diagnostics

Snapshot-local path IDs, stable 2-series physical IDs, related 1-series and
`FD-*` material IDs, and resolved gear type must not be conflated. Duplicate
physical IDs require traceable, blocking validation diagnostics rather than
silent merging or skipping.

Core already had `@RetroId`, whose documentation promised uniqueness within an
archive when used with `@RetroFact`. Before this issue, the implementation only
validated that gear types used a consistent ID definition, that only one ID
field existed per gear type, and that a fact-backed ID was required.

Issue 24 completes that contract. A fact-backed `@RetroId` may now be optional:
tagged gear remains discoverable while waiting to receive its physical Retro
ID. Identity semantics and requiredness are separate concerns. `@RetroId` means
"unique when present," while `@RetroFact(optional = ...)` controls whether
absence prevents construction.

Uniqueness is not a parsing responsibility. `FactParser` should remain
deterministic and stateless: the collection-model `RetroId` parser can recognize
`2xxxxx` without doubt, while a separate archive-scoped validation phase
registers the resulting values and reports duplicates. A parser-owned registry
would be order-dependent, stateful across rebuilds, difficult to scope, and
unsafe under concurrent resolution.

The collection model's `RetroId` is a value-equal record. Its parser recognizes
exactly the six-digit 2-series namespace. The quickly assembled demo `ID`
remains unchanged and is not reused by the new model.

### 5. Conservative Parsing and Strict Validation

Empty, unknown, and misspelled groups can remain unresolved clues. Missing
information can leave a gear generic. Recognized contradictions and broken
identity invariants are different: they must block resolved gear output until
the collection is corrected.

The repository stores the raw extracted clue archive, not a resolved gear
snapshot. It may therefore stow away a faithful extraction containing duplicate
IDs. Every resolution of that cached archive will fail validation again. This
preserves the rebuildable cache boundary while ensuring invalid gear is never
emitted to a caller.

### 6. Explainable Resolution and Cataloguing Audit

The current `GearMatcher` returns only a `Confidence`, and the resolver keeps
only the winning specialist.

For the first model slice, `MysteryGear` together with its retained clues, facts,
and unassigned attributes may be sufficient explanation. RetroCrawler need not
classify the cause of the mystery. Detailed assessments of rejected matchers
should be added only if the real workflow demonstrates that they are needed,
then exposed through the Issue 22 query boundary.

The audit reports objective resolution state. It does not diagnose whether a
`MysteryGear` requires archive changes or code changes; that judgment remains
with the collector.

### 7. Provenance Through Resolution

Source path, local clues, parsed facts, confidence, unresolved attributes,
ambiguity, validation errors, and identity conflicts must survive gear
resolution. This is directly related to [Issue 22](Issue_22.md).

Issue 24 should expose the concrete provenance requirements. Issue 22 should
then define the general query representation and API. The Vaadin application
should be adapted only after that boundary exists.

## Application Direction

The existing Vaadin application should eventually become a reusable collection
browser rather than be replaced by a second personal application.

The agreed order is bottom-up:

1. Prove and evolve the model against the real collection.
2. Implement the structured query and provenance boundary in Issue 22.
3. Refactor the Vaadin application to consume that boundary.
4. Run a personal deployment of the same application with external
   configuration on the collection host.

GUI work is not part of the initial Issue 24 slice.

## Recommended First Vertical Slice

Graphics cards are the recommended initial gear family.

The graphics-card subtree contains:

- 804 descendant directories.
- 94 ID-tagged card folders.
- AGP, EISA, ISA, PCI, and VLB categories.
- Cards with complete and incomplete local type evidence.
- Untagged gear.
- Brand-grouping folders.
- Drivers, manuals, purchase records, repairs, and other non-gear descendants.
- Cards represented inside complete-system builds.

This is small enough for close inspection but broad enough to exercise the
important framework boundaries.

The first slice should likely contain:

- A shared collection-gear abstraction.
- `GraphicsCard`.
- A minimal `ComputerBuild` or complete-system gear type.
- `MysteryGear` as the model-owned fallback for tagged gear that no specialized
  matcher identifies.
- A folder-name clue finder for the bracket language.
- Separate parsers for physical, scan, and floppy-image identifier namespaces.
- Parsers for bus, display connectors, serial number, lifecycle state, and a
  very small set of high-value facts.
- Conservative weighted matchers in which specialized evidence outranks the
  `MysteryGear` fallback.
- Structured cataloguing findings for missing Retro IDs, mystery gear,
  unresolved tags, ambiguity, and validation failures.
- Preservation of unknown tags and facts.

Hardware vocabulary such as ISA, PCI, AGP, or RAM form factors belongs in the
collection model initially. A reusable optional adapter module should be
extracted only if multiple independent models demonstrate the same need. It
should not be placed in core merely because the vocabulary is common in this
collection.

## Test and Verification Strategy

Automated tests should use a compact synthetic archive containing anonymized
versions of the observed structural cases:

- An untagged folder that remains undiscovered even inside a known taxonomy.
- A tagged card below an AGP taxonomy folder but without sufficient local facts,
  demonstrating that hierarchy does not become durable type evidence.
- A card that repeats its ancestor bus locally.
- A locally tagged AGP card that resolves as a graphics card.
- A locally tagged PCI object that remains generic.
- A locally tagged PCI and VGA object that resolves as a graphics card.
- A valid Retro ID whose available facts leave it as `MysteryGear`.
- A mystery result whose resolution trace shows whether facts were unused or
  all specialized matchers rejected them.
- The same physical ID moved into a build and reclassified solely from the
  facts still present on its folder.
- A lifecycle tag before the title.
- Free title text after a bracket group.
- Empty and unknown groups.
- A complete system containing component gear.
- Documentation and acquisition folders beneath gear that must not become
  gear.
- An irrelevant untagged subtree.
- A missing 2-series ID reported as incomplete cataloguing.
- A repeated 2-series physical ID that blocks resolved gear output.
- A 1-series scan referenced by physical gear without being mistaken for its
  physical identity.
- An `FD-*` floppy-image reference.

Focused core tests must accompany each generalized framework change. The full
reactor must continue to pass with `mvn test`, and packaged module boundaries
should be verified with `mvn clean install` when the new module is integrated.

An explicit local smoke test may crawl selected live subtrees, but it must not
be part of the default build or encode private paths.

## Agreed First Implementation Milestone

The following four steps are explicitly approved and form the next
implementation milestone:

1. Introduce runtime-configurable archive locations while preserving annotation
   defaults for existing models.
2. Add `retro-crawler-mycollection` with a minimal walking model: a shared gear
   abstraction, model-owned `MysteryGear`, `GraphicsCard`, `RetroId`, expansion
   bus, the robust bracket-language clue finder, and one conservative matcher.
3. Complete the existing `@RetroId` archive-wide uniqueness contract with
   traceable duplicate diagnostics.
4. Separate identity semantics from requiredness so a fact-backed `@RetroId`
   may be absent when its `@RetroFact` is optional.

A compact synthetic archive should verify these four steps before the model is
run against a selected live subtree. It should cover an undiscovered untagged
folder, mystery gear with and without a Retro ID, a locally tagged AGP graphics
card, a serial number that resembles an ID, and a duplicate Retro ID reported
with both source locations.

## First Milestone Implementation

The four agreed steps were implemented on 2026-07-26.

### Runtime Archive Locations

`@RetroArchive.locations` now defaults to an empty array. Existing
`Model.from(...)` calls still use and require annotation locations. New
overloads accept an `ArchiveRoots` interface for package discovery, explicit
model types, or a `TypeSource`; those runtime roots override annotation
locations.

`ArchiveRoots` deliberately replaces a bare `Collection<Path>` at the model
boundary. It gives the concept a discoverable home while retaining convenient
factories:

- `ArchiveRoots.from(Path...)`
- `ArchiveRoots.from(Collection<Path>)`
- `ArchiveRoots.load(Path)`

`load` reads a UTF-8 text file containing one path per nonblank line. Factory
implementations are immutable; `ArchiveDescriptor` defensively copies roots
supplied by custom implementations.
The descriptor makes an immutable defensive copy and does not require the
paths to exist during model construction.

`MyCollectionArchive` consequently contains archive identity and clue-finder
configuration but no private filesystem paths.

This milestone implements the framework API, not a persistent local
configuration provider. No production launcher currently calls the runtime
overload for `mycollection`, and no local settings file has been created.
Before a live smoke test or personal deployment, a caller must read the roots
from external configuration and pass them to `Model.from(...)`.

Archive roots exist in the immutable `ArchiveDescriptor` while the process is
running. A crawled `Archive` also records each root as its bucket `basePath`, so
the repository cache contains private paths and must itself be configured
outside the source repository. The cache is derived private data, not the
source of runtime root configuration.

### Collection Module and Walking Model

The reactor now contains `retro-crawler-mycollection`, whose only production
dependency is `retro-crawler-core`. Its namespace is
`com.retrocrawler.mycollection`.

The initial model contains:

- `MyGear`, with optional `RetroId`, optional expansion bus, title, folder name,
  and retained unassigned attributes.
- Model-owned `MysteryGear` using core's existing weak `AnyGearMatcher`.
- `GraphicsCard` with one deliberately conservative matcher: a local AGP fact
  is strong evidence, while PCI and other currently ambiguous buses remain
  `MysteryGear`.
- `ExpansionBus` with AGP, EISA, ISA, PCI, and VLB.
- A value-equal 2-series `RetroId` and exact parser.
- `BracketPathClueFinder`, which ignores paths without an opening bracket,
  retains title text before, between, and after groups, parses named and
  anonymous groups, and preserves empty, unknown, reserved-key, and unmatched
  groups as clues rather than silently discarding them.

No file finder is configured yet because none is needed for this milestone,
not because file clues have different framework status.

### Optional Identity and Archive-Wide Validation

The obsolete prohibition on optional fact-backed `@RetroId` fields was removed.
When a resolved gear has no ID, its field remains absent and no uniqueness
entry is registered.

Resolution now retains the ID value selected for each gear. RetroCrawler
resolves the complete archive, registers present IDs across all buckets, and
collects their source paths. If duplicates exist, it throws
`DuplicateRetroIdException` containing every conflicting value and path before
calling `beginBucket` or otherwise emitting gear to the requested tree factory.
The registry is scoped to one crawl and parsers remain stateless.

### Synthetic Proof

The synthetic collection tests cover:

- An untagged grouping folder that remains undiscovered.
- Identified `MysteryGear`.
- Unnumbered PCI `MysteryGear`.
- A locally tagged AGP `GraphicsCard`.
- A serial-number clue containing a 2-series-looking value that is not parsed
  as a Retro ID.
- Duplicate typed Retro IDs reported with both synthetic source paths.
- Title text following leading tags.
- Empty and unmatched bracket groups retained as clues.
- Annotation locations overridden by immutable runtime configuration.
- Optional fact-backed IDs at core level.

No real collection paths or data are present in the module or its tests.

## File-Derived Collection Clues

A second read-only, filename-only convention sweep was performed on
2026-07-27. It did not read file contents or record private names. In addition
to the metadata files above, it found strong support for these established or
unambiguous conventions:

- `angled.jpeg`, `front.jpeg`, and `back.jpeg` gear photographs.
- 57 filenames beginning with an `FD-*`-shaped floppy-image identifier.
- 4,216 macOS `.webloc` files and 12 Windows `.url` files.

Manuals, drivers, firmware, purchase records, repairs, warranties, and similar
material also occur, but their names are not yet a sufficiently reliable
language. Those plausible conventions are deliberately deferred until the
collection supplies clearer rules. The current chaos is retained as chaos
rather than prematurely modelled.

The collection archive now configures five file-derived clue finders alongside
the bracket path finder:

- `RetroPropertiesClueFinder` imports every legacy Java property as a keyed
  clue.
- `RetroMarkdownClueFinder` reads the complete UTF-8 `retro.md` document into
  the `description` clue.
- `StandardImageClueFinder` recognizes only the exact, case-insensitive
  `angled.jpeg`, `front.jpeg`, and `back.jpeg` conventions.
- `FloppyImageClueFinder` recognizes a numeric `FD-*` prefix, retains both the
  typed `FloppyImageId` and the matching file path, and does not mistake an
  `FD-*` occurrence in the middle of a filename for a declaration.
- `WebReferenceClueFinder` retains the paths of `.webloc` and `.url` files. It
  does not yet parse or dereference their contents.

File clues are scoped to the directory currently being crawled. A
`front.jpeg` directly inside a gear folder belongs to that gear artifact. A
`front.jpeg` inside a nested `Photos` directory belongs to the `Photos`
artifact; RetroCrawler does not move the clue upward or infer ownership from
the parent path.

Consequently, `retro.md` can establish an artifact even when the containing
folder has no bracket tags. This is intentional framework behavior:
folder-name, file-name, and file-content clue finders remain peer discovery
mechanisms. The personal model's practical "no tag, no gear" rule now includes
these deliberately conventional file declarations as tags.

### Corroboration and Conflict

Different clue finders may observe the same semantic key. A hash suffix would
turn one observation into the accidental canonical value and would obscure the
fact that both sources describe the same concept. RetroCrawler therefore keeps
the original semantic key:

- Equal values corroborate and collapse to one value.
- Different values are united under that key.
- A scalar fact parser may resolve multiple raw spellings when they all parse
  to the same value.
- If the values parse to different scalar values, the attribute remains an
  unresolved multi-valued clue. No value is injected into the gear and no
  matcher sees a fact for that key.
- A collection-valued fact may legitimately retain multiple parsed values.

For example, folder tag `[AGP]` together with `bus=AGP` in
`retro.properties` yields the `AGP` bus fact. `[AGP]` together with `bus=PCI`
yields the unresolved clue `bus = {AGP, PCI}` and the gear remains a
`MysteryGear` unless other independent facts identify it.

This policy makes conflict visible without letting clue-finder execution order
choose a winner. Deciding which source is wrong remains a cataloguing task.
Structured conflict diagnostics belong with the later Issue 22 query and audit
work.

## Subsequent Implementation Direction

5. Broaden the graphics-card model only as real tag combinations justify it.
6. Expose `MysteryGear`, retained attributes, and objective validation findings
   as a structured cataloguing audit.
7. Add Markdown notes as an optional file-derived fact. **Completed.**
8. Run explicit local scans over selected IBM subtrees.
9. Fix invalid source data exposed by validation rather than bypassing it.
10. Record mismatches and generalize core one pressure point at a time.
11. Feed the resulting provenance requirements into Issue 22.

## Progress

- [x] Agree on an in-repository, non-privileged personal model module.
- [x] Agree on the `com.retrocrawler.mycollection` namespace.
- [x] Establish the privacy boundary between model code and collection data.
- [x] Perform a read-only structural reconnaissance of both collection roots.
- [x] Identify graphics cards as the first vertical slice.
- [x] Defer the Vaadin refactor until after the real model and Issue 22.
- [x] Establish 2-series physical, 1-series scan, and `FD-*` image identifier
      semantics.
- [x] Establish hierarchy as mutable snapshot containment rather than durable
      type evidence.
- [x] Establish strict validation for broken collection invariants.
- [x] Establish cataloguing audit and explainable unresolved gear as primary
      outcomes.
- [x] Establish "no tag, no gear" and model-owned `MysteryGear` fallback
      semantics.
- [x] Identify archive-wide `@RetroId` uniqueness as an existing but
      unimplemented core contract.
- [x] Approve the four-step first implementation milestone.
- [x] Confirm that folder-name, file-name, and file-content clue finders are
      peer recognition mechanisms; no additional file-clue eligibility
      mechanism is required.
- [x] Add the Maven module.
- [x] Implement the core runtime archive-location override API.
- [x] Add `ArchiveRoots` factories and plain-text root-file loading.
- [ ] Choose/create the actual external root file and private cache location,
      then wire an explicit smoke-test launcher.
- [x] Implement the collection folder-language adapter.
- [x] Implement the first gear types, facts, parsers, and matcher.
- [x] Implement optional IDs and archive-wide duplicate validation before gear
      emission.
- [x] Add legacy properties, Markdown notes, standard-image, floppy-image, and
      web-reference clue finders.
- [x] Define corroborating and conflicting clue semantics without choosing a
      value by source order.
- [x] Add anonymized synthetic fixtures and focused tests.
- [ ] Perform the first explicit live-subtree smoke test.
- [x] Record resulting core changes and verification.

## Open Questions

- Which external configuration format and local location should supply the
  personal archive roots and private repository/cache directory?
- Should ignore policy and subtree-pruning policy be one contract or separate
  extension points?
- Which repeated uses of a 1-series or `FD-*` identifier are valid references,
  and which violate their namespace rules?
- Which tagged folders without a Retro ID should still resolve to a specialized
  gear type, and which should become `MysteryGear`?
- Which combinations of bus, connector, and other facts are strong enough to
  select a specialized type?
- How should equal-confidence specialized matchers be represented without
  arbitrarily selecting the first one?
- What is the minimum matcher-assessment contract needed to explain why a gear
  remained generic without forcing every matcher to produce verbose prose?
- Should the fallback role be expressed by `@RetroGear(fallback = true)`, a
  separate annotation, or another explicit model declaration?
- Which cataloguing findings belong to Issue 24's model proof, and which should
  wait for the public Issue 22 query API?
- Should a failed rebuild leave the previous snapshot retrievable only with an
  explicit stale-state indication?
- Should raw bracket-group ordering be preserved explicitly?
- Which folder irregularities are unknown, incomplete, or invalid, and what
  diagnostic severity belongs to each?
- Does structured relative-path context need a clue-finder API at all, or can
  it remain a query/provenance and traversal concern?
- When the folder-tag grammar proves reusable, should it become a small
  optional adapter module shared by the demo and personal model?

## Verification

Completed on 2026-07-26:

- Focused core tests:
  `mvn -pl retro-crawler-core
  -Dtest=ArchiveRootsTest,ModelTest,RetroIdValidationTest test`
- Focused collection-model tests through the reactor:
  `mvn -pl retro-crawler-mycollection -am
  -Dtest=MyCollectionModelTest,BracketPathClueFinderTest
  -Dsurefire.failIfNoSpecifiedTests=false test`
- Full reactor: `mvn test`
- Clean packaged reactor and module boundaries: `mvn clean install`

All completed successfully. The Java 25 test runtime emitted existing
ArchUnit/Unsafe deprecation warnings, and the collection module reported that
no test logging provider was installed; neither affected the build.

Focused file-clue and conflict tests completed successfully on 2026-07-27:

- Core clue merging and fact cardinality:
  `ArchivePathClueFinderTest`, `FactFinderTest`.
- Collection clue finders:
  `CollectionFileClueFindersTest`.
- End-to-end synthetic collection behavior:
  `MyCollectionModelTest`, including file-only artifact discovery,
  file-derived facts, corroboration, and unresolved folder/properties
  conflicts.
- Full reactor: `mvn test`.
- Clean packaged reactor and module boundaries: `mvn clean install`.

All completed successfully. The existing Java 25/ArchUnit compatibility
warnings and missing collection-module SLF4J provider warning remain
non-failing.

## Out of Scope for the Initial Slice

- Modeling the entire collection taxonomy.
- Crawling every file type for content.
- Copying or checking in real collection data.
- A natural-language or LLM integration in core.
- Implementing the full Issue 22 query API.
- Refactoring or deploying the Vaadin application.
- Automatically repairing folder names or collection IDs.
