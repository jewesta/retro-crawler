# Issue 29: General Code-Review Follow-up

## Context

A general review found a set of dependency, build, API-surface, and
documentation improvements. The architecture and module boundaries are sound;
this issue addresses the contained maintenance findings without broadening into
CI policy or deciding the long-term home of the personal collection adapter.

The review's numbered findings are retained below so the implementation and
verification remain traceable to the original feedback.

## Intent

- Replace the unused Jackson XML stack with the JSON databinding dependency
  actually used by the core.
- Remove stale build configuration and keep the Spring Boot dependency BOM and
  Maven plugin on one coherent version.
- Centralize the shared JUnit test policy while preserving the CLI logging
  provider and supplying test logging only where it is missing.
- Bring the Vaadin demonstration application onto a current, mutually
  compatible Vaadin and Spring Boot baseline.
- Remove an accidental implementation class from the public API surface.
- Document the purpose and boundaries of the core packages.
- Correct two user-visible spelling mistakes.

## Scope

### 1. Core JSON dependency

`retro-crawler-core` uses Jackson annotations and `ObjectMapper`, but no XML
APIs. Replace `jackson-dataformat-xml` with `jackson-databind`. The resulting
core dependency tree must not contain Woodstox or `stax2-api`.

### 2. Maven configuration

The root-managed `spring-boot-maven-plugin` is not dead configuration: Maven's
`spring-boot:*` prefix resolution currently selects it. It is nevertheless
misconfigured at 4.0.1 while the app imports the Spring Boot 3.2.0 BOM.

Use a single Spring Boot version for the app's BOM and plugin, declare the
application-specific plugin in `retro-crawler-app`, and remove the copied CLI
`exec-maven-plugin` configuration from that module. Keep the real CLI plugin in
`retro-crawler-cli`. Keep the test-only image metadata extractor version local
to the sole module that uses it.

### 5. Test dependencies and logging

Make JUnit Jupiter an inherited test dependency of the reactor parent rather
than repeating it in test-bearing modules. Do not turn the CLI's
`slf4j-simple` dependency into a test dependency: it is the CLI's runtime
logging provider. The baseline no-provider warning originates only from the
personal collection tests, so add a test-scoped provider there rather than
introducing a competing provider into Spring Boot application tests.

### 6. Application dependency baseline

Upgrade the Java 21 application as one compatible stack:

- Vaadin 25.2.5.
- Spring Boot 4.1.0.
- Jackson Databind 2.21.4, matching Spring Boot 4.1.0's managed Jackson 2
  baseline while remaining on Jackson's 2.21 LTS line.
- JUnit 6.0.3 and SLF4J 2.0.18, matching the application stack's managed
  versions.

The app must compile, pass its tests, start successfully in development mode,
and complete a production frontend build.

### 7. Implementation visibility

`RetroCrawlerImpl` is constructed only by the package-private default builder.
Make the implementation package-private so callers depend on `RetroCrawler`
and its builder API.

### 8. Core package documentation

Add `package-info.java` documentation for every core package. The documentation
must explain each package's role in the archive-to-clues-to-facts-to-stash
pipeline and distinguish public extension points from implementation support;
it must not imply that every public Java type is an intended extension point.

### Trivia

- Correct `ReroCrawler` to `RetroCrawler` in the project README.
- Correct `Can be deled any time` to `Can be deleted at any time` in the notice
  written into generated temporary folders.

## Deferred Findings

- Finding 3, adding CI, is postponed to a separate issue.
- Finding 4, documenting or relocating `retro-crawler-mycollection`, is
  postponed to a separate issue.
- A future rule that mechanically defines the supported public API surface is
  outside this documentation-focused issue.

## Progress

- [x] Review findings triaged and scope agreed.
- [x] Baseline reactor tests recorded.
- [x] Dependency and build cleanup.
- [x] Application framework upgrade.
- [x] API visibility and package documentation.
- [x] Full verification.

## Baseline Verification

Before implementation, `mvn test` passed on the Issue 29 branch:

- Core: 140 tests.
- Shared model: 61 tests.
- Demo: 2 tests.
- Personal collection adapter: 48 tests.
- Vaadin application: 1 test.

The run produced one expected maintenance finding: the personal collection
model test emitted SLF4J's no-provider warning. No other module emitted that
warning.

## Implementation

### Dependencies and build

- Replaced `jackson-dataformat-xml` with `jackson-databind` in the core and
  removed the unused XML and Woodstox dependency path.
- Introduced shared Jackson, JUnit, and SLF4J version properties in the reactor
  parent. JUnit Jupiter is now an inherited test dependency backed by the
  JUnit BOM instead of being repeated by individual modules.
- Added `slf4j-simple` only to the personal collection adapter's test classpath,
  eliminating its no-provider warning without competing with Spring Boot's
  Logback provider or changing the CLI's runtime provider.
- Moved the image metadata extractor version to the application, its only
  consumer, and updated it to 2.21.0.
- Removed the stale parent-managed Spring Boot plugin and the application's
  copied CLI execution configuration. The application now declares a Spring
  Boot 4.1.0 plugin that matches its imported Boot BOM.

### Application stack

- Upgraded the demonstration application to Spring Boot 4.1.0 and Vaadin
  25.2.5, including the generated frontend manifest and index changes.
- Added the optional `vaadin-dev` dependency required by Vaadin 25 development
  mode and renamed `vaadin.whitelisted-packages` to
  `vaadin.allowed-packages`.
- Replaced the deprecated local-file `StreamResource` construction with an
  inline `DownloadHandler`.
- Replaced the deprecated theme annotation and Vaadin 24 theme directory with
  explicit Lumo and application stylesheets served from standard web
  resources. The `retro-crawler` theme name and stylesheet content are
  preserved; only their Vaadin loading mechanism and resource root change.
- Replaced the ID column's invalid `file://` anchor after Vaadin 25.2's safe-URL
  validation exposed it during a live browser session. The core tree factory
  now offers source paths through a backward-compatible overload, and the
  Vaadin tree retains each Gear's real archive path in an app-specific row.
- The ID is now a link-styled local action that validates the canonical target
  against the configured archive roots and asks the host desktop to open the
  folder. The Spring Boot demo explicitly disables Java headless mode so this
  desktop integration is available; failures remain visible as an in-app
  notification. This is intentionally a local-demo capability, not a promise
  that remote Vaadin clients can open their own filesystems.

### API and documentation

- Made `RetroCrawlerImpl` package-private; callers continue to construct the
  public `RetroCrawler` API through its builder.
- Added domain-oriented package documentation to every core package, including
  the archive, clues, fact resolution, Gear construction, stash, progress, and
  implementation-support boundaries.
- Corrected both review trivia typos.

## Final Verification

All checks ran with Temurin Java 21:

- `mvn clean install` passed for all seven reactor modules: 140 core tests, 61
  shared-model tests, 2 demo tests, 48 personal-collection tests, and 1
  application test (252 total). The baseline SLF4J no-provider warning is no
  longer emitted.
- After repairing local folder opening, `mvn test` passed the complete reactor
  with 141 core tests, 61 shared-model tests, 2 demo tests, 48
  personal-collection tests, and 3 application tests (255 total). The new
  tests cover factory source-path delivery, canonical folder opening, and
  rejection of targets outside configured archive roots.
- `mvn -pl retro-crawler-app -am -Pproduction clean install` passed and the
  Vaadin plugin found both application stylesheet declarations.
- `mvn spring-boot:run` resolved Spring Boot Maven Plugin 4.1.0, started the
  application successfully on Java 21 in Vaadin development mode, and returned
  HTTP 200 from `/`; the rendered page referenced the `retro-crawler`
  stylesheet.
- A real Vaadin browser session reached `Index ready`, rendered all nine Gear
  rows and their images without a push failure, and an automated ID click
  opened the selected demo directory through the local desktop without a
  server warning or browser error.
- The core dependency tree contains Jackson Databind 2.21.4 and no Jackson XML,
  Woodstox, or StAX2 dependencies. The application resolves SLF4J 2.0.18,
  Logback 1.5.34, and JUnit Jupiter 6.0.3 as intended.
- The canonical formatter assertion passed for all 16 changed Java files with
  no changes required.
- `git diff --check` passed.

The pre-existing unchecked-operation compiler note in `GearResolverFactory`
remains outside this issue's agreed review scope.
