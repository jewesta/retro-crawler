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
import com.retrocrawler.core.archive.clues.ClueSourceKind;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.archive.clues.DuplicateClueException;

class UnifiedClueFinderTest {

	@TempDir
	private Path root;

	@Test
	void combinesIndependentFindersInConfigurationOrder() throws IOException {
		final ClueFinder first = folder -> Clues.of(Clue.of("folder observation"));
		final ClueFinder second = folder -> Clues.of(Clue.of("file observation"));

		final ArchiveNode archive = digger(first, second).dig(root, new Journal());

		assertNotNull(archive.artifact());
		assertEquals(List.of("folder observation", "file observation"), archive.artifact().clues().stream()
				.filter(Clue::isAnonymous).flatMap(clue -> clue.value().stream()).toList());
	}

	@Test
	void rejectsTwoFindersClaimingOneKey() {
		final ClueFinder first = folder -> Clues.of(Clue.of("bus", "ISA"));
		final ClueFinder second = folder -> Clues.of(Clue.of("bus", "PCI"));

		final ClueFindingException failure = assertThrows(ClueFindingException.class,
				() -> digger(first, second).dig(root, new Journal()));

		assertEquals(ClueSourceKind.FOLDER_VIEW, failure.source().orElseThrow().kind());
		assertInstanceOf(DuplicateClueException.class, failure.getCause());
		assertTrue(failure.getMessage().contains("bus"), failure.getMessage());
	}

	@Test
	void failLateContinuesWithTheNextIndependentFinder() {
		final IllegalStateException randomFailure = new IllegalStateException("Unexpected finder failure.");
		final ClueFinder broken = folder -> {
			throw randomFailure;
		};
		final AtomicBoolean invoked = new AtomicBoolean();
		final ClueFinder working = folder -> {
			invoked.set(true);
			return Clues.of(Clue.of("image", "front.jpeg"));
		};
		final Journal journal = new Journal(FailureMode.FAIL_LATE);

		assertThrows(CrawlException.class, () -> digger(broken, working).dig(root, journal));

		assertTrue(invoked.get());
		assertEquals(1, journal.failureCount());
		final ClueFindingException recorded = assertInstanceOf(ClueFindingException.class,
				journal.failures().getFirst());
		assertSame(randomFailure, recorded.getCause());
		assertEquals(ClueSourceKind.FOLDER_VIEW, recorded.source().orElseThrow().kind());
	}

	@Test
	void letsSeveralFindersInspectTheSameFile() throws IOException {
		Files.writeString(root.resolve("front.jpeg"), "shared evidence");
		final AtomicInteger inspections = new AtomicInteger();
		final ClueFinder first = contentFinder("first", inspections);
		final ClueFinder second = contentFinder("second", inspections);

		final ArchiveNode archive = digger(first, second).dig(root, new Journal());

		assertEquals(2, inspections.get());
		assertEquals(Set.of("shared evidence"), archive.artifact().clues().get("first").orElseThrow().value());
		assertEquals(Set.of("shared evidence"), archive.artifact().clues().get("second").orElseThrow().value());
	}

	@Test
	void reportsTheExactFileWhenAFinderInspectedOnlyThatFile() throws IOException {
		final Path gear = Files.createDirectory(root.resolve("Example Board"));
		Files.writeString(gear.resolve("retro.md"), "broken metadata");
		final ClueFinder broken = folder -> folder.files().stream().filter(file -> "retro.md".equals(file.name()))
				.findFirst().flatMap(file -> file.peek(UnifiedClueFinderTest::rejectMetadata)).orElseGet(Clues::none);

		final ClueFindingException failure = assertThrows(ClueFindingException.class,
				() -> digger(broken).dig(root, new Journal()));

		assertEquals(ClueSourceKind.FILE_CONTENT, failure.source().orElseThrow().kind());
		assertEquals("retro.md", failure.source().orElseThrow().name());
		assertTrue(failure.getMessage().startsWith("[unified_clue_finder_test] Example Board/retro.md: "
				+ broken.getClass().getSimpleName() + " read the file content."), failure.getMessage());
	}

	private ClueFinder contentFinder(final String key, final AtomicInteger inspections) {
		return folder -> folder.files().stream().filter(file -> "front.jpeg".equals(file.name())).findFirst()
				.flatMap(file -> file.peek(content -> {
					inspections.incrementAndGet();
					return Clues.of(Clue.of(key, read(content)));
				})).orElseGet(Clues::none);
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
}
