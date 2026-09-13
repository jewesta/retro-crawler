# Development and demos

This document covers the local toolchain, canonical formatting, and bundled
demonstration applications. Start with the [README](../README.md) for the
project overview.

## Prerequisites

- Electricity
- Java 21 or newer
- Apache Maven
- Node 25 for the Vaadin demo application only

## Build and test

Run the normal multi-module test suite from the repository root:

```sh
mvn test
```

Use a clean packaged build when module boundaries or produced artifacts need
verification:

```sh
mvn clean install
```

The `run/tests.sh` and `run/tests.bat` launchers provide the project's bundled
test entry points.

## Java cleanup and formatting

RetroCrawler uses the headless Java formatter from the separate
[Westarps devtools](https://github.com/jewesta/devtools) repository. It first
applies a conservative OpenRewrite cleanup, including import sorting and
unused-import removal, and then formats the result with Eclipse JDT. The
canonical `prettify/formatting-rules.xml` profile in devtools can also be
imported into Eclipse or STS.

By default, the launcher expects `devtools` beside `retro-crawler`. Override
the location through `WESTARPS_DEVTOOLS_HOME` or repository-local Git
configuration:

```sh
git config --local westarps.devtools.path /path/to/devtools
```

Check every tracked Java source without writing changes:

```sh
run/prettify.sh --assert
```

Clean up and format selected files:

```sh
run/prettify.sh --apply path/to/First.java path/to/Second.java
```

The matching `run/prettify.bat` launcher provides the same interface on
Windows. Maven preparation is enabled by default so OpenRewrite can resolve
types correctly; `--no-prepare` skips that build when reactor outputs are
already current.

## Vaadin demo application

The `retro-crawler-app` module demonstrates how the library fits into a GUI and
can serve as a starting point for another application. It is intentionally
less stable than `retro-crawler-core` and may change as the framework evolves.

![RetroCrawler Demo App](images/retro_crawler_demo_app.png)

From the repository root on macOS or Linux:

```sh
cd run
./app.sh
```

On Windows:

```batch
cd run
app.bat
```

Once the application starts, open `http://localhost:8080`.

The Archive selector offers the Retro PC demo through both a local directory
and its adjacent ZIP file. Selecting an entry and pressing **Reindex** shows
that both providers produce the same collection. Image previews are read
through the selected `ArchiveSource`; opening a local Archive folder is
available only for the filesystem variant.

Demo material is copied below the working directory in `rc_demo_archives` when
needed. RetroCrawler creates a local `cache` directory for its JSON clue
Archive. Both generated directories are ignored by Git.

## Command-line demo

The CLI demonstrates the same collection without Vaadin or Node. On macOS or
Linux:

```sh
cd run
./cli.sh
```

Use `cli.bat` on Windows. It is intentionally small: the purpose is to show the
framework API without hiding it behind a user interface.
