package com.retrocrawler.core.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.CrawlException;
import com.retrocrawler.core.FailureMode;
import com.retrocrawler.core.Journal;
import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.ClueFinder;
import com.retrocrawler.core.archive.clues.ClueFindingException;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.archive.clues.DuplicateClueException;

class UnifiedClueFinderTest {

	@TempDir
	private Path root;

	@Test
	void combinesIndependentFindersInConfigurationOrder() throws IOException {
		final ClueFinder first = new FirstTestClueFinder(folder -> Clues.of(folder.clue("folder observation")));
		final ClueFinder second = new SecondTestClueFinder(folder -> Clues.of(Clue.of("file observation")));

		final ArchiveNode archive = digger(first, second).dig(root, new Journal());

		assertNotNull(archive.artifact());
		assertEquals(List.of("folder observation", "file observation"), archive.artifact().clues().stream()
				.filter(Clue::isAnonymous).flatMap(clue -> clue.value().stream()).toList());
		assertEquals(List.of("FirstTestClueFinder", "SecondTestClueFinder"), archive.artifact().clues().stream()
				.filter(Clue::isAnonymous).map(clue -> clue.finder().orElseThrow()).toList());
		assertEquals(List.of(ari(Path.of(""))),
				archive.artifact().clues().stream().filter(Clue::isAnonymous).findFirst().orElseThrow().sources());
		assertTrue(archive.artifact().clues().stream().filter(Clue::isAnonymous).skip(1).findFirst().orElseThrow()
				.sources().isEmpty());
	}

	@Test
	void rejectsTwoFindersClaimingOneKey() {
		final ClueFinder first = new FirstTestClueFinder(folder -> Clues.of(folder.clue("bus", "ISA")));
		final ClueFinder second = new SecondTestClueFinder(folder -> Clues.of(folder.clue("bus", "PCI")));

		final ClueFindingException failure = assertThrows(ClueFindingException.class,
				() -> digger(first, second).dig(root, new Journal()));

		assertEquals(ari(Path.of("")), failure.source().orElseThrow());
		assertEquals("SecondTestClueFinder", failure.finder().orElseThrow());
		assertInstanceOf(DuplicateClueException.class, failure.getCause());
		assertTrue(failure.getMessage().contains("observed by FirstTestClueFinder from " + ari(Path.of(""))),
				failure.getMessage());
		assertTrue(failure.getMessage().contains("observed by SecondTestClueFinder from " + ari(Path.of(""))),
				failure.getMessage());
	}

	@Test
	void failLateContinuesWithTheNextIndependentFinder() {
		final IllegalStateException randomFailure = new IllegalStateException("Unexpected finder failure.");
		final ClueFinder broken = new FirstTestClueFinder(folder -> {
			throw randomFailure;
		});
		final AtomicBoolean invoked = new AtomicBoolean();
		final ClueFinder working = new SecondTestClueFinder(folder -> {
			invoked.set(true);
			return Clues.of(Clue.of("image", "front.jpeg"));
		});
		final Journal journal = new Journal(FailureMode.FAIL_LATE);

		assertThrows(CrawlException.class, () -> digger(broken, working).dig(root, journal));

		assertTrue(invoked.get());
		assertEquals(1, journal.failureCount());
		final ClueFindingException recorded = assertInstanceOf(ClueFindingException.class,
				journal.failures().getFirst());
		assertSame(randomFailure, recorded.getCause());
		assertEquals(ari(Path.of("")), recorded.source().orElseThrow());
		assertEquals("FirstTestClueFinder", recorded.finder().orElseThrow());
	}

	@Test
	void letsSeveralFindersInspectTheSameFile() throws IOException {
		Files.writeString(root.resolve("front.jpeg"), "shared evidence");
		final AtomicInteger inspections = new AtomicInteger();
		final ClueFinder first = contentFinder("first", inspections, true);
		final ClueFinder second = contentFinder("second", inspections, false);

		final ArchiveNode archive = digger(first, second).dig(root, new Journal());

		assertEquals(2, inspections.get());
		assertEquals(Set.of("shared evidence"), archive.artifact().clues().get("first").orElseThrow().value());
		assertEquals(Set.of("shared evidence"), archive.artifact().clues().get("second").orElseThrow().value());
		assertEquals(List.of(ari(Path.of("front.jpeg"))),
				archive.artifact().clues().get("first").orElseThrow().sources());
	}

	@Test
	void reportsTheExactFileWhenAFinderInspectedOnlyThatFile() throws IOException {
		final Path gear = Files.createDirectory(root.resolve("Example Board"));
		Files.writeString(gear.resolve("retro.md"), "broken metadata");
		final ClueFinder broken = new TestClueFinder(
				folder -> folder.files().stream().filter(file -> "retro.md".equals(file.name())).findFirst()
						.flatMap(file -> file.peek(UnifiedClueFinderTest::rejectMetadata)).orElseGet(Clues::none));

		final ClueFindingException failure = assertThrows(ClueFindingException.class,
				() -> digger(broken).dig(root, new Journal()));

		assertEquals(ari(Path.of("Example Board", "retro.md")), failure.source().orElseThrow());
		assertEquals("TestClueFinder", failure.finder().orElseThrow());
		assertTrue(
				failure.getMessage().startsWith(
						ari(Path.of("Example Board", "retro.md")) + ": TestClueFinder failed while finding clues."),
				failure.getMessage());
	}

	@Test
	void rejectsTheSameFinderClassTwice() {
		final ClueFinder first = new TestClueFinder(folder -> Clues.none());
		final ClueFinder second = new TestClueFinder(folder -> Clues.none());

		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> digger(first, second));

		assertTrue(failure.getMessage().contains("Ambiguous clue finder name 'TestClueFinder'"), failure.getMessage());
	}

	@Test
	void rejectsDifferentFinderClassesWithTheSameSimpleName() {
		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> digger(new FirstNamespace.AmbiguousFinder(), new SecondNamespace.AmbiguousFinder()));

		assertTrue(failure.getMessage().contains("Ambiguous clue finder name 'AmbiguousFinder'"), failure.getMessage());
		assertTrue(failure.getMessage().contains(FirstNamespace.AmbiguousFinder.class.getName()), failure.getMessage());
		assertTrue(failure.getMessage().contains(SecondNamespace.AmbiguousFinder.class.getName()),
				failure.getMessage());
	}

	@Test
	void rejectsAnUnnamedFinderBecauseItsIdentityCannotBeStored() {
		final ClueFinder lambda = folder -> Clues.none();

		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> digger(lambda));

		assertTrue(failure.getMessage().contains("must be a named class"), failure.getMessage());
	}

	private ClueFinder contentFinder(final String key, final AtomicInteger inspections, final boolean first) {
		final java.util.function.Function<com.retrocrawler.core.archive.clues.ArchiveFolderView, Clues> finding = folder -> folder
				.files().stream().filter(file -> "front.jpeg".equals(file.name())).findFirst()
				.flatMap(file -> file.peek(content -> {
					inspections.incrementAndGet();
					return Clues.of(file.clue(key, read(content)));
				})).orElseGet(Clues::none);
		return first ? new FirstTestClueFinder(finding) : new SecondTestClueFinder(finding);
	}

	private static String read(final InputStream content) {
		try {
			return new String(content.readAllBytes(), StandardCharsets.UTF_8);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static Clues rejectMetadata(final InputStream content) {
		throw new IllegalStateException("Could not parse metadata.");
	}

	private ArchiveDigger digger(final ClueFinder... finders) {
		final ArchiveDescriptor descriptor = new ArchiveDescriptor(ArchiveId.of("unified_clue_finder_test"),
				"Unified clue finder test", root);
		return new ArchiveDigger(new TestArchiveDefinition(descriptor, List.of(finders)));
	}

	private static ARI ari(final Path resourcePath) {
		return ARI.of("test_collection", ArchiveId.of("unified_clue_finder_test"), resourcePath);
	}

	private static final class FirstNamespace {

		private static final class AmbiguousFinder implements ClueFinder {

			@Override
			public Clues find(final com.retrocrawler.core.archive.clues.ArchiveFolderView folder) {
				return Clues.none();
			}
		}
	}

	private static final class SecondNamespace {

		private static final class AmbiguousFinder implements ClueFinder {

			@Override
			public Clues find(final com.retrocrawler.core.archive.clues.ArchiveFolderView folder) {
				return Clues.none();
			}
		}
	}
}
