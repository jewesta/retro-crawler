# RetroCrawler

**RetroCrawler** is a Java framework for *structurally crawling* directory-based archives and turning them into typed domain objects so you can manage your stash of retro gear.

If you are like us then you have your collection organized as files and folders. This is simple, pragmatic and backup-friendly. Because of this, RetroCrawler is designed specifically for collections that were **not originally structured as databases** — such as retro computer hardware documentation (pictures, manuals, drivers), software archives, ROM libraries or document repositories.

RetroCrawler does not require specific schemas, metadata files, or folder layouts.  
Instead, you can use your own personal already existing folder structure, provide context via `ClueFinder`s, `FactParser`s and `GearMatcher`s and ReroCrawler **infers structure from context** using a two-phase pipeline.

---

## Core Concepts

### Archive
A directory tree on disk that serves as the data source.

### Artifact
An optional representation of a single folder in the archive.
An artifact exists only if meaningful information can be extracted from that folder which we call `Clue`s. This is a raw representation of a single **potential** piece of your collection.

### Clue
A raw key–value observation derived from:
- folder names
- file names
- file contents

Clues are always **string-based** and may contain multiple values. This is a raw representation of a **potential** property of a piece in your collection.

### Gear
A user-defined domain object created from a set of facts. This is an **identified**, real piece in your collection.
Gear types are **not** required to implement framework interfaces and require only a no-arg constructor. It's "bring your own type".

### Fact
A typed interpretation of a clue.
Facts are produced by user-defined parsers and may be any Java type.

---

## Processing Pipeline

RetroCrawler operates in two distinct phases:

### 1. Archive / Clue Phase
The archive is traversed recursively.
For each folder, registered `ClueFinder`s extract clues and produce an `Artifact`.

Artifacts are cached as JSON on the local storage so that expensive rescans can be avoided. Once a scan is done, queries on the archive are blazingly fast. If you restart your app, the archive is quickly loaded from the JSON cache.

### 2. Gear / Fact Phase
All known clues are converted into facts using registered parsers.
Based on these facts, `GearMatcher`s determine which gear type best represents an artifact.
The corresponding `GearFactory` then creates the final domain object.

Unknown or unparseable clues are preserved and may be accessed explicitly.

---

## Configuration via Annotations

RetroCrawler is configured entirely via annotations:

- `@RetroArchive`  
  Declares archive locations and clue-finder configuration.

- `@RetroGear`  
  Declares a gear type and its matcher.

- `@RetroFact`  
  Declares how a field is populated from a clue.

- `@RetroAnyAttribute`  
  Captures all remaining unassigned facts. Especially useful on "catch all" default gear types that are produced if none others match.

This allows the framework to remain strongly typed while requiring minimal boilerplate.

---

## Design Goals

- No database
- No imposed interfaces on user domain models
- Strong typing without forcing predefined structures
- Incremental and cacheable processing
- Minimal core dependencies
- Suitable for both CLI tools and GUI applications (e.g. Vaadin)

---

## Prerequisites

- Electricity
- Java 21+
- Apache Maven
- Node v25 (Vaadin demo app only)

---

## Demo App

Since RetroCrawler is a library, we provide a demo app based on the Vaadin UI framework so you can see how all comes together. You can use this as a starting point for building your own gui. But please note that compared to `retro-crawler-core` keeping `retro-crawler-app` stable is not a priority. Anything might change any time.

You can run the demo app via a provided shell script (macOS) or batch file (Windows). CD into `/run` located in the root of the repository. Then run the script. This should build and install RetroCrawler and launch the Vaadin app. Once it runs you can access it via `localhost:8080`. The demo scenario is called "Retro PC" and the archive (data folder) it is based on is located at `/retro-crawler-app/archives/retro_pc`. RetroCrawler will create a folder `retro-crawler-app/cache` where the JSON cache file is located. This folder is on the Git ignore list.

#### macOS
```sh
cd run
./app.sh
```

#### Windows
```batch
cd run
app.bat
```

---

## Status

RetroCrawler is currently under active development.
APIs may evolve, but we try to keep core concepts stable.