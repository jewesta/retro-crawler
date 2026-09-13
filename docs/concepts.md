# Concepts and processing

This document describes RetroCrawler's archive vocabulary, evidence rules, and
two-phase processing model. For the project overview and runnable examples,
start with the [README](../README.md).

## Archive

An Archive is a separately identified, hierarchical collection holding rooted
at exactly one place. The default source is a local directory tree, but
providers may expose ZIP entries, remote files, or other file-like hierarchies
through the same archive model.

One `RetroCrawler` applies a shared model to every archive registered with it.
Each archive keeps its own identity, root, source provider, repository entry,
and crawl lifecycle. A collection spread across several disks, mounts, or
media is therefore composed as several Archives rather than as several roots
of one Archive. Every access or crawl resolves a complete Stash across them
and validates Retro ID uniqueness across the collection.

## Artifact

An Artifact is the optional raw representation of one folder. It exists only
when a `ClueFinder` extracts meaningful information from that folder. At this
point RetroCrawler has found a potential collection piece, not yet a typed
Gear object.

Artifacts contain Clues, never Facts. This separation lets a changed model
reinterpret a stored clue archive without physically crawling its source
again.

## Clue

A Clue is a model-independent string observation derived from a folder name,
file name, or file content. It may have an explicit key and one or several
values, or it may be anonymous when the source states no semantic key.

Every crawled Clue retains the unique simple class name of the finder that
produced it. A finder may also declare the exact archive resources that
contributed to that Clue. Those sources are stable ARIs, never physical paths.
The source list is optional: no declared source means that the finder makes no
resource-level provenance claim, and merely accessing a resource never causes
RetroCrawler to infer one.

### One authority per key

Within one Artifact, every explicit clue key has exactly one authority. One
Clue may contain several values, but separate Clues from different finders
must not claim the same key. RetroCrawler rejects that archive inconsistency
even when the values agree; source order never chooses a winner and separate
authorities are never silently merged.

Anonymous Clues deliberately claim no semantic key. Any number of finders may
contribute anonymous observations to one Artifact, and they survive under
distinct generated keys. Resolution may later interpret several anonymous
observations as values of the same Fact. Once an anonymous observation's
meaning is recognized, however, it must not compete with an explicitly keyed
Clue for that semantic key.

## Clues and accumulation

`Clues` is the immutable, observation-ordered collection of Clues found at one
archive location. The type itself carries the one-authority rule between a
finder, the crawler, and an Artifact. Build it directly with `Clues.of(...)`
or accumulate observations one at a time:

```java
final ClueAccumulator clues = Clues.accumulator();
clues.add(folder.clue("bus", "AGP"));
clues.add(folder.clue("Example Graphics Board"));
return clues.clues();
```

`ArchiveFolderView` and `ArchiveFileView` carry authoritative ARIs. Their
`clue(...)` factories attach that resource to the new Clue. A finder that does
not want to make an exact source claim uses `Clue.of(...)` instead. Sources
for an aggregate Clue are added deliberately while the finder loops over its
contributors; there is no bulk operation that assigns every accessed resource
to every returned Clue.

A second Clue claiming an occupied key is rejected with a
`DuplicateClueException` at the moment it is observed.

## Diagnostics and evidence boundaries

Because RetroCrawler rejects a conflict instead of merging it, a failed crawl
has to say where. Every clue failure leaves a crawl as a
`ClueFindingException` with a compiler-style header naming an authoritative
resource ARI and the finder. A failure inside `ArchiveFileView.peek(...)`
names that exact file; otherwise the candidate folder is the accurate
default. The original condition remains available as the cause.

A finder that tracks offsets can hand them over so the rejection points at the
text the cataloguer actually wrote:

```text
ari:/my_collection/hardware/Graphics%20Cards/Example%20Board: BracketClueFinder failed while finding clues.
Duplicate clue key 'bus'. One artifact may contain only one clue for a key. First values: [ISA], duplicate values: [PCI].
  Example Board [bus ISA] [200001] [bus PCI]
                ^^^^^^^^^ first
                                   ^^^^^^^^^ duplicate
```

Pass a `ClueLocation` while accumulating:

```java
clues.add(Clue.of(key, values),
        ClueLocation.in(folderName, openingBracket, length));
```

When conflicting Clues come from different finders, their durable finder and
source provenance are both reported. Text positions travel with the `Clues`
a finder returns:

```text
ari:/my_collection/hardware/Graphics%20Cards/Example%20Board: RetroMarkdownClueFinder failed while finding clues.
Duplicate clue key 'bus'. One artifact may contain only one clue for a key. First values: [AGP], duplicate values: [PCI].
  The first clue was observed by BracketClueFinder from ari:/my_collection/hardware/Graphics%20Cards/Example%20Board, line 1, column 15.
    Example Board [bus AGP]
                  ^^^^^^^^^
  The duplicate clue was observed by RetroMarkdownClueFinder, line 3, column 1.
    bus: PCI
    ^^^
```

Positions stop at the Artifact. A retrieved Clue retains source ARIs but no
snapshot of source text, because a cached offset could point into content that
has since changed.

A finder may cite only the candidate folder or an exact resource present in
its pruned `ArchiveFolderView`. A path merely being below the Artifact is not
enough: a pruned child Artifact belongs to another potential collection piece
and is outside the readable evidence boundary. If any returned Clue declares
an invalid source, the finder's complete result is rejected before any of it
is accumulated.

Every finder receives its candidate only after the children have been
classified. Its view contains the candidate's name, its direct files, and
only child folders positively established as clue-free metadata folders.
Child Artifacts and failed folders are structurally absent, preventing one
finder from crossing into another Gear's evidence.

## Fact

A Fact is a typed, model-dependent interpretation produced by a `FactParser`.
A Fact may use any Java value type and retains the source Clue from which it
was resolved. Unknown or unparseable Clues remain available explicitly rather
than disappearing.

## Gear

Gear is a user-defined domain object created from resolved Facts. It is the
identified, real piece in the collection. Gear types do not implement a
framework interface and require only a no-argument constructor: it is a
bring-your-own-type model.

Gear relationships follow archive location. Folder ancestry describes where
Gear currently lives, not what it is; matchers must establish identity from
the Artifact's own evidence.

## Processing pipeline

### 1. Archive and Clue phase

RetroCrawler traverses each Archive recursively. Registered `ClueFinder`s run
once per candidate folder and produce an Artifact when meaningful evidence is
present. The resulting clue archive is stowed through the selected
`Repository`, avoiding an expensive rescan when only the model changes.

Finders are independent and may inspect the same resources. The one-authority
rule governs the Clues they are allowed to return.

### 2. Gear and Fact phase

All known Clues are offered to the model's parsers. `FactParser`s produce
typed Facts, `GearMatcher`s evaluate the complete detection Fact set, and the
selected `GearFactory` creates and populates the Gear object.

Resolution produces a complete immutable Stash only after the configured
Archives have succeeded. Read [Modeling and querying](modeling.md) for model
discovery, matching, traces, and typed queries.
