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

- `retro-crawler-mycollection` depends on `retro-crawler-core` and the optional
  shared `retro-crawler-model`.
- `retro-crawler-core` must not depend on or reference the personal model.
- `retro-crawler-core` must not depend on `retro-crawler-model`; the shared
  vocabulary is a consumer of the same public framework API.
- The model uses only public framework APIs.
- Collection-specific tag names, subjective cataloguing state, aliases,
  parsers, and matchers remain outside the core and shared model.
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

### Known Keys Without Values

Empty syntax and an intentionally placed key marker are not the same thing.
The agreed interpretation is:

| Observation | Meaning |
|---|---|
| `[]` | No clue; ignore it completely |
| `[SN]` | The known `sn` key was deliberately placed but has no value yet |
| `[AGP]` | An anonymous value clue because `agp` is not a model key |
| `[SN 123]` | An explicitly keyed value clue |

A bare observation already establishes the artifact independently of the
model. During resolution, a bare observation that matches a known key is
interpreted as a **missing-value clue**. It:

- Establishes that the containing folder is an artifact.
- Allows derived metadata such as the folder title to be retained.
- Keeps the key and source provenance for a later cataloguing audit.
- Does not become a fact and therefore leaves the corresponding gear field
  absent.
- Can be superseded by a real value for the same key from another clue finder.

This is a core resolution semantic rather than collection-specific model
knowledge. Individual clue finders remain unaware of the model vocabulary.
After all model types have been reflected, `GearResolver` centrally compares
anonymous, single-valued clues with the known `@RetroFact` keys. An unambiguous
case-insensitive match is reclassified under the canonical model key with no
values for that resolution only.

The cached archive retains the raw finder observation. In the bracket example,
it stores anonymous `SN`, not a model-dependent `sn` marker, and resolution
does not mutate the `Artifact`. Adding, removing, or renaming a fact key can
therefore reinterpret an existing cache without crawling the filesystem again.
The derived missing-value clue remains available in the resolved gear's
unassigned attributes for cataloguing audits and user interfaces.

The public clue representation can also express an explicit missing-value clue
emitted directly by a finder, for example when a keyed file format contains a
declared key with no value. Such a raw clue is represented in JSON as
`"<key>": []` and survives repository round trips. This is the established
version 1 representation: the reader already interpreted an empty value array
as a keyed clue with zero values. The corresponding writer path is now covered
explicitly, and no archive-format version change is required.

Neither `ArchiveDigger`, `ArchivePathClueFinder`, nor an individual configured
finder receives or exposes the model's known keys.

This deliberately reserves known keys against anonymous use: if `foo` is a
model key, bare `foo` means that the value for `foo` is missing. A literal value
with the same spelling must be expressed under an explicit key. This avoids a
second, open-ended registry of known values.

The personal bracket adapter handles the syntactically empty `[]` separately.
It emits neither an empty clue nor a title derived solely from that empty
group. This preserves the collection's "no tag, no gear" rule while allowing a
nonempty marker such as `[SN]` to establish unfinished gear.

### Clues and Facts Are Separate Levels

The cache-stability discussion exposed a general framework invariant:

> A clue is model-independent evidence. A fact is a model-dependent
> interpretation of that evidence. Archives persist clues, never facts.

The dependency deliberately points in only one direction. `Artifact` contains
raw `Clue` instances; `Fact` retains the source `Clue` from which its typed value
was derived. The archive and repository layers do not depend on gear
resolution. Consequently, adding or changing model types, fact keys, parsers,
or matchers can reinterpret a stored archive without crawling the filesystem.

A clue finder may preserve a key that is explicitly present in its source
format, such as a property name. It must not obtain the model's known key set,
create facts, or decide which gear type an artifact represents. Resolution may
derive an effective clue view for the active model, as it does for a bare
`[SN]` marker, but that view is not written back into the artifact.

This contract is formalized in several ways:

- The repository-wide design principles state the clue/fact boundary.
- Public Javadocs define the responsibilities of `ClueFinder`, `Clue`,
  `Artifact`, `Fact`, and `GearResolver`.
- Archive packages are architecture-tested against dependencies on gear
  resolution packages.
- Artifacts and clues defensively protect their cached evidence from external
  mutation.
- A fact must contain at least one interpreted value and protects that value
  from external mutation.
- The model-evolution test proves that resolution can reinterpret the same
  cached artifact without replacing or changing it.

Separate `RawClue` and `ResolvedClue` types are deliberately not introduced. A
clue may be observed directly or derived during resolution while remaining a
clue. The important boundary is between cached evidence and model-dependent
interpretation, not between two species of clue.

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

The initial recommendation was to leave hardware vocabulary such as ISA, PCI,
AGP, and RAM form factors in the collection model until real use justified an
extraction. The demo and the live collection model subsequently supplied that
evidence, leading to the optional `retro-crawler-model` module documented
below. The vocabulary remains outside core.

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

The reactor now contains `retro-crawler-mycollection`, whose production
dependencies are `retro-crawler-core` and the optional shared
`retro-crawler-model`. Its namespace is `com.retrocrawler.mycollection`.

The initial model contains:

- `MyGear`, with optional `RetroId`, optional expansion bus, title, folder name,
  and retained unassigned attributes.
- Model-owned `MysteryGear` using core's existing weak `AnyGearMatcher`.
- `GraphicsCard` with one deliberately conservative matcher: a local AGP fact
  is strong evidence, while PCI and other currently ambiguous buses remain
  `MysteryGear`.
- Shared `ExpansionBus` vocabulary with AGP, EISA, ISA, PCI, and VLB.
- A value-equal 2-series `RetroId` and exact parser.
- `BracketPathClueFinder`, which ignores paths without an opening bracket,
  retains title text before, between, and after groups, parses named and
  anonymous groups, ignores syntactically empty groups, and preserves unknown,
  reserved-key, and unmatched groups as clues rather than silently discarding
  them.

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
- A serial-number key marker without a value that establishes `MysteryGear`,
  retains its title and remains a clue rather than becoming a fact.
- Duplicate typed Retro IDs reported with both synthetic source paths.
- Title text following leading tags.
- Empty bracket groups ignored and unmatched bracket groups retained as clues.
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

The sweep also counted 4,216 macOS `.webloc` files and 12 Windows `.url`
files. Their prevalence initially looked like a convention, but the first live
crawl disproved that interpretation: most merely record acquisition or
research sources in supporting subfolders. Presence alone carries no useful
gear meaning.

Manuals, drivers, firmware, purchase records, repairs, warranties, and similar
material also occur, but their names are not yet a sufficiently reliable
language. Those plausible conventions are deliberately deferred until the
collection supplies clearer rules. The current chaos is retained as chaos
rather than prematurely modelled.

The collection archive now configures four file-derived clue finders alongside
the bracket path finder:

- `RetroPropertiesClueFinder` imports every legacy Java property as a keyed
  clue. The collection's files are decoded explicitly as UTF-8 rather than
  through `Properties.load(InputStream)` and its ISO-8859-1 interpretation.
- `RetroMarkdownClueFinder` reads the complete UTF-8 `retro.md` document into
  the `desc` clue. The short key is also the established legacy-properties
  spelling, while the Markdown convention keeps it implicit for collectors.
- `StandardImageClueFinder` recognizes only the exact, case-insensitive
  `angled.jpeg`, `front.jpeg`, and `back.jpeg` conventions.
- `FloppyImageClueFinder` recognizes a numeric `FD-*` prefix, retains both the
  typed `FloppyImageId` and the matching file path, and does not mistake an
  `FD-*` occurrence in the middle of a filename for a declaration.

The initial `WebReferenceClueFinder` was removed after the live smoke test.
Bookmarks may only return as clues if a future adapter can derive meaning
beyond "there is a bookmark."

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

## Bounded Crawl Planning and Progress

A complete counting pass would make percentage progress exact, but it would
also traverse the entire filesystem before clue extraction could begin. On a
large archive that analysis could itself appear to hang. Reporting only the
current path proves that the crawler is alive, but gives no numerical sense of
movement.

The agreed compromise is a bounded, shallow analysis sweep that partitions the
archive into approximate work regions:

1. Start with the configured archive roots as the frontier.
2. List one complete directory level.
3. Replace each non-leaf frontier directory with its child directories.
4. Stop once the frontier contains enough regions, or a configured depth,
   analyzed-directory, or elapsed-time bound is reached.
5. Crawl each frontier subtree as one approximate region.

The default `CrawlPlanning` configuration targets 100 regions and bounds
analysis to six levels, 2,000 analyzed directories, or five seconds between
level expansions. Applications can supply an explicit configuration through
`RetroCrawler.Builder.crawlPlanning(...)`.

The analysis listings are retained and reused by the actual crawl. Besides
avoiding immediate duplicate filesystem reads, this makes one crawl operate on
a coherent shallow directory snapshot if files are added while it runs.

Region progress is intentionally labelled approximate. One region may contain
many more folders or more expensive clue files than another. Human-readable
messages therefore continue to include the current path while the structured
event reports completed and total regions. The UI must not describe this as an
exact elapsed-work percentage.

The original RC-specific `Monitor`, `CrawlProgress`, and
`CrawlCancelledException` were subsequently folded into one core progress
abstraction rather than retained behind adapters. `Progressor` now owns
control, observation, cancellation, timing, and structured snapshots:

- `ProgressStage` identifies the active phase. The supplied archive stages are
  `PLANNING`, `CRAWLING`, `STOWING`, and `RESOLVING`, while applications may
  define their own stages.
- `ProgressAccuracy` distinguishes indeterminate, approximate, and exact
  units.
- `ProgressSnapshot` freezes the active stage, message, units, accuracy,
  terminal state, root fraction, elapsed time, and a simple remaining-time
  estimate.
- `ProgressMonitor` observes snapshots; `Progressor.reportingMessages(...)`
  is the deliberately small convenience bridge for CLI and GUI consumers that
  only need human-readable status.
- `FixedStepProgressMonitor` throttles determinate observers to a chosen number
  of steps without hiding stage or state transitions.
- A progressor can be split into nested, weighted windows. This supports later
  multi-step operations without hard-coding one global percentage model into
  the crawler.
- `COMPLETE`, `CANCELLED`, and `FAILED` are terminal states rather than
  pseudo-stages. A failed crawl publishes `FAILED` before propagating the
  original exception.

The implementation is a dependency-free adaptation of progressor work from
Relimit GmbH, used with permission and attributed in the class-level comments.
Pepper-specific translation, logging, Spring, Jackson, console, and pipeline
support was not copied into core. The resulting API retains the useful
weighted-progress and snapshot concepts while making RC's stage accuracy and
abortive cancellation first-class.

Planning is indeterminate, crawling is numerically approximate, and resolution
is exact. Once the extracted archive exists in memory, RetroCrawler counts its
artifacts cheaply and reports exact resolved/total progress. Stages in the
current crawl replace one another and therefore report phase-local root
fractions. Weighted child progressors are available when a caller wants to
compose several operations into one deliberate overall percentage.

Cancellation is thread-visible and abortive. It raises
`ProgressCancelledException` at crawl checkpoints rather than returning
placeholder nodes. `ArchiveManager` consequently never stows a partially
crawled archive. A cancellation arriving after a complete archive has already
been stowed may still prevent resolution or emission, but the raw cache itself
remains valid.

The existing Vaadin demo and CLI consume the message view of the same
`Progressor`; the private smoke runner consumes structured snapshots. The
Vaadin demo creates a fresh progressor for each refresh so a cancelled run does
not poison a later crawl. Rendering the structured snapshot as a dedicated
progress component remains part of the later application work.

## First Live Subtree Smoke Test

The first explicit live crawl ran successfully on 2026-07-27 against the
graphics-card subtree selected during reconnaissance. The root file and JSON
repository were created outside the source repository, and neither the private
root nor cache contents were copied into this issue record. A read-only
preflight found no symbolic links in the selected subtree.

The bounded planner divided 804 directory nodes into 168 approximate crawl
regions. Observable checkpoints included 129/168, 152/168, and 168/168 before
the crawler entered `STOWING`. Resolution then reported exact progress through
231/231 artifacts and finished successfully. The complete Maven-launched run
took 28.4 seconds and produced a 212 KiB JSON clue cache.

The resolved, privacy-safe aggregate was:

- 231 gear: 7 `GraphicsCard` and 224 `MysteryGear`.
- 94 gear with a Retro ID and 137 without one.
- 45 gear folders with at least one standard image.
- 99 gear folders with web references.
- No Markdown descriptions or floppy images in this subtree.

The seven specialized graphics cards are locally AGP-tagged artifacts. The
model correctly did not infer their type merely from the selected subtree or
its organizational parent folders.

The live data also exposed a concrete admission problem. Ninety-three of the
231 artifacts were established solely by web-reference clues, typically in
nested research or acquisition-support folders. Two more were established
solely by standard-image clues. Because the model deliberately supplies
`MysteryGear` as its fallback, these artifacts become gear and inflate the raw
"missing Retro ID" count. That count is therefore not yet a trustworthy
cataloguing task list.

This is not evidence that file clue finders are second-class or that file
clues should only corroborate folder tags. It demonstrates that bookmark
presence is not a clue at all for this model. The `WebReferenceClueFinder` and
its `webReferences` fact were therefore removed. If bookmark support returns,
its adapter must extract enough meaning to distinguish a useful declaration
from an acquisition or research reference. The cache did its job: it made this
bad convention measurable without changing the source archive.

After removing that finder, the same subtree was rebuilt from source. Planning
again found 804 nodes and 168 approximate regions, while the resolved result
fell from 231 to 138 artifacts:

- 7 `GraphicsCard` and 131 `MysteryGear`.
- 94 gear with a Retro ID and 44 without one.
- 45 gear folders with standard images, including 3 established by images
  alone.
- No `webReferences` keys anywhere in the rebuilt cache.

The 93-artifact reduction exactly matches the folders previously established
solely by bookmark presence. The specialized type and Retro ID counts remained
unchanged, demonstrating that the removal discarded noise rather than known
gear. The rebuilt private cache is 166 KiB. This second crawl took 48.2
seconds; the unchanged region count but different elapsed time reinforces why
region progress is explicitly approximate.

`MyCollectionSmokeCrawl` is the explicit local launcher. It reads an
`ArchiveRoots` text file and private cache directory from command-line
arguments, can either rebuild or reuse the cache, streams structured progress
in five-percent buckets, and prints only privacy-safe aggregate cataloguing
totals at completion. Structured progress messages retain the current path for
local operator visibility, but no runtime path is compiled into the launcher.

## First Full IBM-Compatible Crawl

The model was released against the complete IBM-compatible archive on
2026-07-27. It used a separate external root file and private repository so the
focused graphics-card cache remained available as a comparison baseline. A
read-only preflight found no symbolic links.

The planner produced 519 approximate regions. Crawling completed every region,
stowed a 1.8 MiB JSON archive, and then reported exact resolution through all
2,197 artifacts. The cache contains 6,365 directory nodes. The complete
Maven-launched run took 3 minutes 48 seconds.

Archive-wide Retro ID validation then deliberately stopped gear emission. It
found:

- 17 duplicated Retro ID values.
- 35 total folder occurrences.
- 16 two-way collisions and one three-way collision.

This is the intended garbage-in/garbage-out result. No duplicate was silently
selected, no hierarchy-based exception was invented, and no collection folder
was changed. The fully extracted clue cache remains usable for repeated
resolution, but source corrections require a rebuild before validation can
pass.

The smoke launcher now catches `DuplicateRetroIdException`, writes the complete
value-and-path details to an owner-readable private report beside the cache,
prints only aggregate validation counts, and still exits unsuccessfully so
automation cannot mistake an invalid catalogue for a successful result. The
report path and its contents remain external runtime data and are not recorded
in this repository.

After the collector corrected the first set of source findings, the complete
archive was rebuilt rather than resolved from stale cache. The second pass
completed 518 approximate regions, stowed 6,362 directory nodes, and resolved
2,193 artifacts. It then found:

- 9 duplicated Retro ID values, down from 17.
- 18 total folder occurrences, down from 35.
- Only two-way collisions; the previous three-way collision was corrected.

Thus eight duplicate values were eliminated by the source corrections. The
remaining nine pairs still prevent gear emission. The second run took 9
minutes 54 seconds because one approximate region experienced a long
filesystem delay; it eventually completed without intervention. The private
report was replaced with the current findings and retained owner-only
permissions.

## First Fact-Mining Slice

The first fact-mining pass was implemented on 2026-07-27 against the existing
private full-archive cache. The cache still contains each archive node's
original folder name, so it can prove candidate parsing conventions without
requiring another expensive source traversal. A later reindex is nevertheless
required before the stored clues themselves use the new key normalization and
comma grammar.

### Folder-Language Corrections

The first cache inspection exposed a concrete parser defect: the original
comma splitter treated decimal commas as list separators. Values such as
`3,5`, `1,44MB`, `3,3V`, and `1,125MB` were consequently damaged before a
fact parser could see them.

The collection grammar now treats a comma followed by whitespace as a list
separator. A comma without following whitespace remains part of one value.
This applies equally to anonymous and keyed groups: a list such as
`[ISA, PCI]` still yields two values, while `[Set 2 x 1,125MB]` retains one
complete set declaration.

The bracket finder must understand syntax, not collection vocabulary. It
therefore contains no list of known keys and does not translate `trw` into a
semantic field name. Every syntactically valid keyed group is normalized to a
lowercase key by the same case-insensitive grammar rule. Thus `TRW` becomes
`trw`, `SN` becomes `sn`, and an unknown `Alias` becomes `alias`.

The model then binds typed facts explicitly to folder-language keys such as
`trw` and `set`. A subsequent shared-model extraction added a validated
`MacAddress` fact and retained serial numbers as collection-owned strings
because their syntax remains manufacturer-specific.

### Typed Facts

The initial pass added three value types and exact parsers:

- `TheRetroWebId` represents a positive numeric database ID. It is not a
  physical identity and is therefore not annotated with `@RetroId`; two
  collection objects may legitimately refer to the same database entry.
- `DataCapacity` represents a positive decimal quantity in KB, MB, GB, or TB,
  supports decimal commas, comparison across units, and multiplication.
  Lowercase `kb` is normalized to KB because the live archive uses it for byte
  capacities on SIMMs, cache modules, disks, and memory chips; capitalization
  is not a reliable bit/byte distinction in this folder language.
- `RamSet` represents a count-first declaration such as `2 x 16MB`, exposes
  the capacity per member, and derives the total capacity. The property is
  named `memberCount`, rather than `stickCount`, because the live archive also
  applies the set convention to loose memory components.

`TheRetroWebId` and `DataCapacity` were subsequently promoted into
`retro-crawler-model`; `RamSet` and its folder-language parser remain owned by
the personal collection.

The Retro Web deep-link category is deliberately not guessed by the ID parser.
An ID alone does not say whether the target belongs below `motherboards`,
`expansioncards`, or another site route. The shared model therefore separates:

- `TheRetroWebId`, the positive numeric component parsed from a clue.
- `TheRetroWebCategory`, The Retro Web's public content taxonomy and route
  names. This is deliberately distinct from RetroCrawler's gear taxonomy.
- `TheRetroWebReference`, the unambiguous combination of category and ID.

`TheRetroWebReference.lookupUri()` constructs the widely used numeric lookup
route, such as `https://theretroweb.com/expansioncards/10510`. This route is
undocumented and redirects to the canonical slug page, but a site maintainer
has confirmed awareness of external reliance on numeric IDs. URI construction
is deterministic and performs no network access or redirect resolution.

The collection-side `TheRetroWebReferences` adapter supplies the category only
after gear resolution. It maps recognized graphics cards and motherboards onto
`EXPANSION_CARD` and `MOTHERBOARD` references respectively; generic and mystery
gear retain the ID without inventing a potentially incorrect link. Neither the
gear types nor the shared reference type depends on the other model. The
adapter deliberately owns knowledge of both, in the same way that a watch
accepts a time without time having to know about watches.

Reverse-order and embellished set spellings are left unresolved. The parser
does not quietly reinterpret them, because the agreed collection convention is
count first and obvious inconsistencies should be corrected at the source.

### Private-Cache Coverage

Aggregate mining of the 6,362-node private cache found:

- 234 The Retro Web observations: 231 positive numeric IDs parse exactly and
  three explicit non-numeric placeholders remain unresolved.
- 30 named MAC-address observations, all accepted and normalized by the shared
  `MacAddress` grammar.
- 424 standalone KB/MB/GB/TB capacity observations, all accepted by the
  capacity grammar.
- 163 `Set` observations: 149 use the canonical count-first form, 12 use the
  reverse form, and two are more complex. The latter 14 remain clues.
- All 149 canonical RAM sets also declare a standalone total capacity.
  147 derived totals agree with that declaration; two conflict.
- No folder name or bracket group in this cache contains an explicit or
  checksum-valid ISBN. ISBN remains in the shared model because the preexisting
  demo parser records prior collection-domain intent and because arbitrary
  unrecognized filenames and file contents are outside this cache's evidence.

The two conflicts are useful catalogue findings, not parser exceptions to be
papered over. They remain in the private source for the collector to inspect
and correct. This is the first example beyond duplicate Retro IDs where typed
facts expose a cross-fact consistency check that the later structured audit can
report.

No private path, folder name, identifier, or cache content was copied into the
repository during this mining pass.

## Shared Model Extraction

The existing demo model was built from the same collection vocabulary and
therefore represents prior domain knowledge, not disposable sample noise. The
live collection also exposed objective facts useful to other collectors. These
two sources now meet in the optional `retro-crawler-model` module.

The shared-model inclusion rule is:

> A fact belongs in the shared model when its meaning is stable outside one
> collector's archive and other collectors can interpret it objectively.

This includes formal standards such as ISBN, industry vocabulary such as ISA
and PCI, and durable community reference systems such as The Retro Web. It
excludes personal identity schemes, subjective condition, workflow state,
collection clue syntax, matchers, and concrete fallback gear.

The initial package structure is deliberately organized by what a fact is,
rather than every collection domain in which it may be used:

```text
com.retrocrawler.model
├── hardware
├── identifier
└── measurement
```

The first shared vocabulary contains:

- `ISBN`, `MacAddress`, and `TheRetroWebId`, with canonical parsers.
- `TheRetroWebCategory` and category-qualified `TheRetroWebReference`, with
  deterministic numeric lookup-URI construction.
- `ExpansionBus`, including AGP, EISA, ISA, MCA, PCI, PCI Express, and VLB.
- `DataCapacity`.
- `MemoryFormFactor` and `MemoryFeature`.
- Separate `MemoryAccessTime` and `MemoryStandard` types. The old demo
  `RAMSpeed` enum was not copied because it conflated nanosecond access times
  with PC66/PC100/PC133 standards.

Parsers live beside their value types. They know canonical textual
representations but do not know collection clue keys or folder syntax. The
demo and personal collection independently bind their own keys to these shared
facts.

The personal model no longer has a generic `.model` package:

- `RetroId` and `FloppyImageId` live under
  `com.retrocrawler.mycollection.catalog`.
- `RamSet` lives under `com.retrocrawler.mycollection.memory`.
- Shared facts are imported from `com.retrocrawler.model`.

The demo package root was shortened from `com.retrocrawler.demo.collection` to
`com.retrocrawler.demo`. Its subjective and demonstration-only state lives
under `com.retrocrawler.demo.catalog`; reusable facts were removed in favor of
the shared model. The demo bracket adapter now normalizes every named key
uniformly and contains no collection-vocabulary switch.

## Second Fact-Mining Slice

A macOS update removed the earlier clue archive because it had been placed in
OS-managed temporary storage. The collection itself was unchanged. A complete
rebuild planned 518 crawl regions, visited 6,362 nodes, and stowed 2,193
artifacts in 3 minutes 6 seconds. Resolution then reproduced the same nine
duplicate Retro ID values and 18 occurrences. Future long-lived private caches
should use a persistent user cache directory; root configuration belongs in a
persistent application-support location. Both remain external to the
repository.

The rebuilt archive contains 4,032 anonymous clues comprising 4,121
observations and 2,045 distinct raw values. Aggregate mining exposed a
cardinality error in the personal model: 151 artifacts resolve at least one
expansion bus, and 40 of those resolve two or three buses. Expansion buses are
therefore a set on `MyGear`, as they already were in the demo, rather than a
single optional value.

More importantly, the real archive disproved the first graphics-card shortcut:
AGP alone is not a sufficient signature. Of 25 AGP-tagged artifacts, 16 are in
the motherboard area, seven are in the graphics-card area, and two are
elsewhere. Archive location was used only to audit the proposed tag-only
matcher; it was not added as model evidence.

Four tag-only signatures were tested with clue-level resolution semantics:

- one expansion bus plus a video connector, without an explicit computer
  form factor, identifies four graphics cards in the current archive;
- an expansion bus plus a computer form factor identifies 28 motherboards,
  including one board moved outside the motherboard area;
- capacity plus either a RAM-module form factor or a memory standard identifies
  226 memory modules, including 34 modules stored inside boards, controllers,
  or computer builds;
- a watt value plus a computer form-factor tag identifies 12 power
  supplies, including two moved outside the power-supply area.

These matchers use facts on the artifact itself. The folder tree serves only as
an independent test oracle during this private mining pass. The results are a
direct proof of the intended behavior: moved gear remains recognizable without
turning parent folders into type evidence.

The shared model gained objective vocabulary and conservative parsers for:

- computer form factors shared by boards, cases, and power supplies;
- video connectors;
- electrical power;
- RAM module form factors and features;
- PC2100, PC2700, and PC3200 memory standards in addition to the existing
  PC66/PC100/PC133 vocabulary.

The personal model binds those facts and adds `Motherboard`, `MemoryModule`,
and `PowerSupply` gear. `GraphicsCard` now requires its stronger tag
combination. `TheRetroWebReferences` derives motherboard links only after a
motherboard has been recognized.

The cache contains six 1-series scan observations representing five distinct
scans. One scan is legitimately referenced by two different physical manuals.
The personal `ScanId` is consequently a reusable fact, not `@RetroId` identity
and not an archive-wide uniqueness constraint.

The same pass exposed cataloguing findings that remain deliberately unresolved:

- four artifacts contain more than one 2-series ID candidate: three contain two
  candidates and one contains five;
- 163 bracket groups are empty;
- 42 bracket groups contain a serial-number marker without a value;
- eight groups contain a bare The Retro Web marker and three named TRW values
  are non-numeric placeholders;
- 27 `EDOFPM` observations initially did not say whether EDO or FPM was
  intended;
- 18 otherwise plausible motherboard signatures keep form-factor and
  power-connector semantics in the same comma-separated clue, so partial
  interpretation is correctly refused.

No parser guesses around these cases. They belong in the later structured
cataloguing audit, and obvious source inconsistencies should be corrected in
the collection.

No private root, path, folder name, identifier value, report, or cache content
was copied into the repository during this pass.

### First photo-assisted catalogue correction

The 27 `EDOFPM` observations were subsequently audited read-only against the
existing standard images. Every affected folder contains a `front.jpeg`, a
`back.jpeg`, and one additional photograph. The front images make the module
or DRAM part numbers readable, so the filesystem archive itself provides the
starting evidence without requiring physical retrieval of the modules.

Manufacturer data sheets and product guides resolve 26 observations directly:
16 are Fast Page Mode and ten are EDO. Evidence includes explicit module
labels, documented DRAM access modes, and manufacturer module descriptions.
This also caught a useful counterexample to marketplace folklore: two Samsung
module families that are frequently advertised as EDO are explicitly Fast
Page Mode in Samsung's own material.

The remaining K-branded `IC418165CJ` module is a strong EDO inference from its
`1M x 16` `418165` family designation and the documented meaning of the same
family number across contemporary manufacturers, but an exact manufacturer
data sheet has not yet been found. It remains a separately identified
confidence case rather than being silently treated as certain.

The audit therefore produced 16 `FPM` corrections and 11 `EDO` corrections,
with one of the latter clearly marked as inferred. After collector review, all
27 corrections were applied to the source archive. Each folder now also carries
one `IC` clue for the module's main RAM chip; chip-only free-text labels became
`Unidentified`, while OEM and module labels were preserved. The independent
chip-designation transcription error was corrected at the same time.

Post-change verification found all 27 destination folders, all 27 front images,
all 27 back images, and no remaining `EDOFPM` placeholder in the audited
directory. A private reversal manifest was kept outside the repository. No
private path, folder name, collection identifier, report, or cache content was
copied into this issue record.

### Completing the unclassified SIMM-72 entries

After removing the explicit `EDOFPM` placeholders, 19 additional module
folders in the same SIMM-72 subtree still carried neither an `EDO` nor an
`FPM` clue. These were audited against their existing photographs rather than
classified from speed, capacity, date, or marketplace convention.

Eighteen entries expose a readable main RAM-chip designation or an explicit
module label. Manufacturer data sheets resolve these directly. The remaining
module uses opaque encapsulated packages, but its acquisition photographs tie
it to a specific contemporary motherboard and that manufacturer's manual
explicitly requires Fast Page Mode DRAM. This provides concrete provenance
without inventing an IC designation.

The audit resolved 17 entries as Fast Page Mode and two as EDO. It caught
another worthwhile nomenclature trap: a Samsung `4103` Quad-CAS family used by
one OEM module is documented as Fast Page Mode despite being described as EDO
in some parts listings. The manufacturer's data sheet won.

All 19 corrections were applied to the source archive. Readable main RAM-chip
designations were normalized into one `IC` clue, chip-only labels became
`Unidentified`, existing short labels were preserved, and one manufacturer
spelling error was corrected. Recursive before-and-after inventories verified
that no contained file or subdirectory changed. A final audit found no
SIMM-72 module folder left without either an `EDO` or `FPM` clue. A private
reversal manifest remains outside the repository; no private path, folder
name, identifier value, or photograph was copied into this issue record.

The same photographs and manufacturer speed-grade documentation also establish
the access time of 18 of these 19 modules: ten are 60 ns and eight are 70 ns.
Those folders now carry the corresponding speed clue immediately after their
main `IC` clue. The opaque-package module remains deliberately untagged:
motherboard documentation establishes its memory technology and compatibility
requirements, but not the module's actual access time. Post-change verification
found all 18 destination folders and confirmed that the unresolved module was
unchanged. A separate private reversal manifest remains outside the repository.

### Partial archive re-indexing

The catalogue corrections made a full-archive rebuild unnecessarily expensive:
the clue archive was stale only below one stable ancestor. Core now represents
indexing intent explicitly with `ReindexScope` instead of a boolean:

- `none()` reuses a stored archive when available;
- `all()` rebuilds every configured archive root;
- `subtree(Path)` and `subtrees(...)` rebuild selected existing folders and
  everything below them.

There is deliberately no boolean compatibility API. All core, test, demo CLI,
Vaadin, and personal-collection consumers use the explicit scope.

A subtree rebuild requires an existing complete clue archive. Requested paths
are normalized, must belong to exactly one configured root, and must identify
nodes already present in the stored archive. Nested requested paths are reduced
to their outermost ancestor. Consequently a renamed folder is refreshed by
selecting its stable stored parent: rebuilding that parent removes the old
cached child, discovers the new child, and also accounts for additions and
deletions below the selected boundary.

The digger now keeps the configured archive root distinct from the selected
crawl path. Synthetic gear IDs therefore remain based on the same
archive-root-relative path during both complete and partial indexing. Fresh
subtrees replace their immutable stored nodes by rebuilding only the ancestor
chain; unrelated buckets and branches are retained. The resulting complete
archive is still stowed through the repository's atomic whole-archive
replacement. Cancellation, crawl failure, or stowaway failure leaves the
previous in-memory and stored archive intact.

The private smoke launcher accepts
`--reindex-subtree <path> [<path>...]`. Its first real use rebuilt only the
corrected memory-module subtree. Planning produced 71 approximate regions, the
merged archive was stowed, and subsequent archive-wide resolution still
processed 2,193 artifacts and reproduced the same nine known duplicate Retro
ID values and 18 occurrences. This demonstrates the intended boundary:
filesystem clue extraction is partial, while resolution and uniqueness
validation continue to operate over the complete merged archive. No private
root, selected path, folder name, cache path, or identifier value is recorded
here.

### Post-order metadata-tree clues

Collection use exposed a structural clue source that the original local finder
contracts could not express: a gear folder may contain a metadata folder such
as a marketplace name, and that folder may in turn contain a conversation or
invoice whose contents describe the gear. The metadata folders are not
artifacts because they are not potential collection parts. Allowing individual
finders to traverse the filesystem would nevertheless surrender crawl
planning, cancellation, archive boundaries, and predictable I/O.

Core now offers an additive `TreeClueFinder` contract. It receives a transient,
read-only `ArchiveFolderView` with direct `ArchiveFileView`s and recursively
navigable metadata folders. File content remains lazy and can be inspected
through crawler-owned `peek(...)`; no filesystem `Path` is exposed through the
new API. All clues returned by a tree finder belong to the current folder.

The digger retains the original pre-order timing of path-name, file-name, and
file-content finders. It then descends depth-first, establishes every child,
constructs the current folder view, runs tree finders post-order, and finally
establishes the current artifact. Any child that established an artifact is
removed from the navigable view but retained unchanged in the persistent
`ArchiveNode` tree. This makes artifact folders opaque clue boundaries while
letting metadata folders remain inspectable to arbitrary depth.

The new views are crawl-time objects and do not alter the repository JSON
format, `Artifact`, resolution, or facts. Tree finders are configured through
`@RetroArchive.LookAt(trees = ...)`; a tree finder alone may establish an
artifact. Existing finders remain valid without modification.

Focused tests prove post-order inspection, lazy nested-file access,
tree-only annotation configuration, local/tree clue merging, artifact
establishment by a tree finder, and pruning of a child artifact together with
its metadata subtree.

Verification completed successfully on 2026-07-29:

- focused tree-discovery and crawl-planning tests;
- all 96 core tests, including the library-use boundary checks;
- the complete eight-module `mvn test` reactor;
- the clean packaged reactor through `mvn clean install`.

One indexing rule remains deliberately explicit for this first slice: when a
stored parent artifact contains clues derived from a metadata descendant,
callers must re-index that artifact folder rather than only the deeper metadata
folder. Precise upward invalidation or automatic widening can be added after
real collection use establishes the desired policy.

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
- [x] Choose/create an external root file and private cache location,
      then wire an explicit smoke-test launcher.
- [x] Implement the collection folder-language adapter.
- [x] Implement the first gear types, facts, parsers, and matcher.
- [x] Implement optional IDs and archive-wide duplicate validation before gear
      emission.
- [x] Add legacy properties, Markdown notes, standard-image, and floppy-image
      clue finders; remove the bookmark-presence finder disproved by the live
      crawl.
- [x] Define corroborating and conflicting clue semantics without choosing a
      value by source order.
- [x] Add bounded shallow crawl planning and approximate numerical region
      progress to core.
- [x] Add structured crawl phases and exact artifact-resolution progress while
      retaining a lightweight message-consumer view.
- [x] Make cancellation abort without stowing a partial archive.
- [x] Fold the crawl monitor, progress event, and cancellation controller into
      an attributed, weighted `Progressor` API in core.
- [x] Add anonymized synthetic fixtures and focused tests.
- [x] Perform the first explicit live-subtree smoke test.
- [x] Crawl the complete IBM-compatible archive and report duplicate Retro IDs
      without changing the source collection.
- [x] Correct decimal-comma handling and normalize named folder keys uniformly
      without teaching the clue finder collection vocabulary.
- [x] Add typed The Retro Web, data-capacity, and RAM-set facts with conservative
      parsers.
- [x] Mine the private full-archive cache for parser coverage and RAM-set total
      consistency without copying private data into the repository.
- [x] Extract objective and durable community vocabulary into the optional
      `retro-crawler-model` module.
- [x] Remove consumer `.model` packages and give personal concepts semantic
      package homes.
- [x] Shorten the demo namespace to `com.retrocrawler.demo` and make the demo a
      real consumer of the shared model.
- [x] Model category-qualified The Retro Web references and derive expansion-
      card lookup links in a collection-side adapter only after gear
      recognition.
- [x] Repeat private-cache mining after the temporary cache was purged by an OS
      update.
- [x] Correct expansion-bus cardinality and replace the disproved AGP-only
      graphics-card matcher.
- [x] Add tag-only motherboard, memory-module, and power-supply vertical slices.
- [x] Add reusable 1-series scan references without treating them as identity.
- [x] Audit all `EDOFPM` placeholders from their existing photographs and
      prepare evidence-backed `EDO`/`FPM` corrections without changing the
      source collection.
- [x] Apply the 27 catalogue corrections, normalize the main RAM chip into one
      `IC` clue per folder, and retain a private reversal manifest.
- [x] Classify and normalize the remaining 19 SIMM-72 module folders, verify
      their contents after renaming, and reduce the unclassified count to zero.
- [x] Add 18 photo- and data-sheet-backed SIMM-72 access-speed clues while
      leaving the one unprovable speed unset.
- [x] Replace boolean re-indexing with `ReindexScope`, implement safe subtree
      archive replacement, and validate it against the private archive.
- [x] Preserve bare key markers as artifact-establishing raw clues and derive
      missing-value clues during resolution while keeping the cache and clue
      finders model-ignorant.
- [x] Formalize and enforce the separation between cached clues and resolved
      facts.
- [x] Record resulting core changes and verification.

## Open Questions

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
- How should artifacts with multiple otherwise valid identity candidates be
  surfaced when their singular `@RetroId` fact correctly remains unresolved?
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

Focused missing-value clue verification completed on 2026-07-29:

- `ClueClassifierTest` covers central, case-insensitive reclassification
  of a known anonymous key, rejection of genuinely empty observations, and
  replacement of a marker by a real value from another finder.
- `CacheModelEvolutionTest` proves that a raw anonymous clue cache built with
  one model is reused without replacement and reinterpreted after a later model
  adds the corresponding `@RetroFact` key.
- `FactFinderTest` proves that a clue without values cannot become a fact.
- `JsonFileRepositoryTest` proves that a missing-value clue is stored using the
  established empty-array representation in an archive marked as version 1 and
  survives a JSON repository round trip.
- `BracketPathClueFinderTest` proves that `[]` contributes neither a clue nor a
  derived title while malformed nonempty syntax remains traceable.
- `MyCollectionModelTest` proves that `[SN]` establishes titled `MysteryGear`
  with no serial-number fact and a retained missing-value clue, while an
  otherwise identical `[]` folder remains undiscovered.

The focused reactor command completed successfully:

`mvn -pl retro-crawler-mycollection -am
-Dtest=CacheModelEvolutionTest,ClueClassifierTest,FactFinderTest,JsonFileRepositoryTest,BracketPathClueFinderTest,MyCollectionModelTest
-Dsurefire.failIfNoSpecifiedTests=false test`

The full reactor `mvn test` and clean packaged reactor `mvn clean install` also
completed successfully.

Focused clue/fact boundary verification completed on 2026-07-29:

- `LibraryUsePolicyTest` prevents the archive packages from depending on gear
  resolution packages.
- `ArtifactTest` proves that cached clue sets and raw clue values cannot be
  modified through caller-owned or returned collections.
- `FactTest` proves that a fact cannot exist without an interpreted value and
  that its typed values cannot be modified externally.
- `CacheModelEvolutionTest` remains the behavioral proof that an unchanged
  archive can be reinterpreted by an evolved model.

The full packaged reactor `mvn clean install` completed successfully after
these boundary contracts were added; the core module ran 85 tests.

The complete IBM-compatible archive was re-indexed on 2026-07-29 after the
empty-group and missing-value semantics were implemented. The persistent roots
file and private JSON repository remained external to the project. Planning
produced 518 approximate regions; crawling, atomic stowaway, and exact
resolution of 2,168 artifacts completed in 4 minutes 28 seconds. Archive-wide
Retro ID validation passed, and the obsolete private duplicate report was
removed.

A read-only aggregate audit of the rebuilt cache and resolved gear confirmed:

- No empty JSON value array remains from a syntactically empty `[]` group.
- 42 case-insensitive bare `SN` observations occur across 41 artifacts and
  resolve to 41 missing-value `sn` clues. One artifact contains the observation
  more than once, while resolved attributes remain keyed per artifact.
- Eight bare `trw` observations resolve to eight missing-value `trw` clues.
- Three named non-numeric `trw` values remain unresolved clues rather than
  being ignored or guessed.

No private root, folder name, identifier, or cache content was added to the
repository while recording these aggregate findings.

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

Focused crawl-planning and progress tests completed successfully on
2026-07-27:

- `ProgressorTest`
- `CrawlPlanningTest`
- `ArchiveDiggerPlanningTest`
- `ArchiveManagerTest`
- `RetroIdValidationTest`
- `RetroCrawlerBuilderTest`

They cover bounded frontier expansion, deep-tree depth limits, reuse of
analysis listings, approximate region completion, exact resolution progress,
builder configuration, message-only observation, structured terminal states,
nested weighted windows, and cancellation without partial repository
replacement.

The full reactor `mvn test` and clean packaged reactor
`mvn clean install` also completed successfully after the progress changes.

The later Relimit progressor adaptation and RC API consolidation were verified
on 2026-07-27:

- `ProgressorTest` covers structured and message-only observation,
  indeterminate/exact/approximate units, simple ETA behavior, fixed-step
  throttling, nested weighted windows, child completion, terminal states, and
  cancellation retaining the active child stage.
- `RetroCrawlerBuilderTest` verifies that a crawl failure publishes `FAILED`
  and still propagates the original exception.
- Existing planning, repository, duplicate-ID, clue-finder, collection-model,
  CLI, and Vaadin consumers compile and test against `Progressor`; there are no
  remaining Java references to the retired RC `Monitor`, `CrawlProgress`, or
  `CrawlCancelledException`.
- The full reactor `mvn test` and clean packaged reactor
  `mvn clean install` both completed successfully.
- The packaged core JAR contains the new `com.retrocrawler.core.progress`
  API and none of the retired progress classes. Privacy and core-library-policy
  scans remained clean.

The first live-subtree smoke test completed successfully on 2026-07-27 using
the external root file and private JSON repository described above. A second
resolution-only invocation using `--reuse-cache` reproduced the same 231-gear
aggregate without crawling or replacing the cache. After adding the launcher
and recording the live findings, both `mvn test` and `mvn clean install`
completed successfully for the full reactor.

Bookmark-clue removal was verified on 2026-07-27 with 11 focused
`MyCollectionModelTest` and `CollectionFileClueFindersTest` tests, including a
regression proving that a bookmark-only folder remains undiscovered. The
corrected live rebuild completed successfully with zero `webReferences` keys
in the JSON archive. The full reactor `mvn test` and `mvn clean install` also
completed successfully afterward.

The first complete IBM-compatible crawl completed extraction and exact
resolution on 2026-07-27, then failed intentionally on archive-wide duplicate
Retro ID validation. A cache-only rerun reproduced all 17 duplicate values and
35 occurrences without another filesystem crawl and generated the private
owner-readable report. After adding that launcher behavior, the full reactor
`mvn test` and `mvn clean install` completed successfully.

The corrected-source full rebuild completed extraction and exact resolution on
2026-07-27 and replaced the private report with the remaining 9 duplicate
values and 18 occurrences. The full reactor `mvn test` and
`mvn clean install` completed successfully after updating stale-report cleanup.

The first fact-mining slice was verified on 2026-07-27 with 19 focused
`BracketPathClueFinderTest`, `CollectionFactParsersTest`, and
`MyCollectionModelTest` tests. They cover decimal commas, whitespace-delimited
lists, uniform key normalization, exact typed parsing, derived RAM-set totals,
end-to-end fact injection, and legitimate reuse of a The Retro Web reference
by different physical gear.

The full reactor `mvn test` and clean packaged reactor
`mvn clean install` also completed successfully after the fact-mining changes.

The shared-model extraction and consumer package cleanup were verified on
2026-07-27:

- Focused shared-model, demo, and collection tests covered identifier,
  hardware, and capacity parsers; demo discovery; uniform bracket-key
  normalization; collection fact parsing; and end-to-end model resolution.
- The full reactor `mvn test` completed successfully.
- The clean packaged reactor `mvn clean install` completed successfully,
  verifying the new `core -> model -> consumers` module boundaries.
- Source scans found no remaining Java references to
  `com.retrocrawler.demo.collection` or `com.retrocrawler.mycollection.model`.
- A repository privacy scan found no collection roots or private cache/report
  paths in source-controlled files.

The category-qualified The Retro Web reference was verified on 2026-07-27:

- Focused `IdentifierParsersTest` coverage verifies motherboard and expansion-
  card numeric lookup URIs.
- Focused `MyCollectionModelTest` coverage verifies that a recognized
  `GraphicsCard` is mapped to an `EXPANSION_CARD` reference from its existing
  `TheRetroWebId`, while mystery gear with the same ID does not produce a link.
- The full reactor `mvn test` completed successfully.
- The clean packaged reactor `mvn clean install` completed successfully.

ArchUnit was upgraded from 1.3.0 to 1.4.2 on 2026-07-28 while retaining
Java 21 as the compiler release target. The focused `LibraryUsePolicyTest` and
the full `mvn test` reactor completed successfully under JDK 25. The upgrade
eliminated both the unsupported class-file-major-version fallback stack traces
and the shaded-Guava `sun.misc.Unsafe` warning previously noted above.

The second private-cache mining slice was verified on 2026-07-29:

- focused shared-model and collection tests cover new hardware vocabulary,
  power parsing, multi-valued expansion buses, tag-only motherboard,
  memory-module, graphics-card, and power-supply recognition, reusable scan
  references, and category-qualified motherboard links;
- cache-only resolution processed all 2,193 artifacts without a parser
  ambiguity, then stopped at the unchanged duplicate-ID gate with nine values
  and 18 occurrences;
- private clue-level analysis reproduced RC's all-or-nothing handling of
  multi-value clues and validated 28 motherboard, 226 memory-module, 12
  power-supply, and four graphics-card signatures without using hierarchy as
  model input;
- the full `mvn test` reactor completed successfully;
- the clean packaged `mvn clean install` reactor completed successfully.

Partial archive re-indexing was verified on 2026-07-29:

- nine focused new core tests cover scope construction, subtree replacement,
  rename handling through a stable parent, preservation of unrelated branches
  and technical IDs, JSON retrieval in a new crawler session,
  missing-base rejection, and failed-stowaway rollback;
- source scans found no remaining boolean crawl or archive-manager indexing
  API;
- the full `mvn test` reactor completed successfully;
- the clean packaged reactor `mvn clean install` completed successfully;
- a private live subtree rebuild crawled 71 approximate regions, stowed the
  merged archive, resolved all 2,193 artifacts, and reproduced the unchanged
  duplicate-ID validation result.

## Out of Scope for the Initial Slice

- Modeling the entire collection taxonomy.
- Crawling every file type for content.
- Copying or checking in real collection data.
- A natural-language or LLM integration in core.
- Implementing the full Issue 22 query API.
- Refactoring or deploying the Vaadin application.
- Automatically repairing folder names or collection IDs.
