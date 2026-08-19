package com.retrocrawler.core.archive.clues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveId;

class ClueDiagnosticsTest {

	private static final ArchiveId ARCHIVE_ID = ArchiveId.of("test_archive");

	@Test
	void locatesAnOffsetWithinASingleLineSource() {
		final ClueLocation location = ClueLocation.in("Example Board [bus PCI]", 14, 8);

		assertEquals(1, location.line());
		assertEquals(15, location.column());
		assertEquals("Example Board [bus PCI]", location.excerpt());
		assertEquals("line 1, column 15", location.describe());
	}

	@Test
	void locatesAnOffsetWithinAMultiLineDocument() {
		final String document = "---\nprice: 120 EUR\nlot: 200001\n---\n";

		final ClueLocation location = ClueLocation.in(document, document.indexOf("lot"), 3);

		assertEquals(3, location.line());
		assertEquals(1, location.column());
		assertEquals("lot: 200001", location.excerpt());
	}

	@Test
	void handlesWindowsLineEndingsWithoutSwallowingTheCarriageReturn() {
		final String document = "---\r\nprice: 120 EUR\r\n";

		final ClueLocation location = ClueLocation.in(document, document.indexOf("price"), 5);

		assertEquals(2, location.line());
		assertEquals("price: 120 EUR", location.excerpt());
	}

	@Test
	void pointsAtBothObservationsWhenTheyShareOneLine() {
		final String folderName = "Example Board [bus ISA] [bus PCI]";
		final ClueAccumulator clues = Clues.accumulator();
		clues.add(Clue.of("bus", "ISA"), ClueLocation.in(folderName, 14, 9));

		final DuplicateClueException failure = assertThrows(DuplicateClueException.class,
				() -> clues.add(Clue.of("bus", "PCI"), ClueLocation.in(folderName, 24, 9)));

		assertEquals("""
			Duplicate clue key 'bus'. One artifact may contain only one clue for a key. \
			First values: [ISA], duplicate values: [PCI].
			  Example Board [bus ISA] [bus PCI]
			                ^^^^^^^^^ first
			                          ^^^^^^^^^ duplicate""", failure.getMessage());
	}

	/**
	 * Two finders claiming one key are drawn against both sources. A finder
	 * accumulates privately, so this only works because {@link Clues} carries
	 * the positions across the hand-off.
	 */
	@Test
	void pointsAtBothSourcesWhenTwoFindersClaimOneKey() {
		final String folderName = "Example Board [bus AGP]";
		final String document = "---\nbus: PCI\n---\n";
		final ClueAccumulator clues = Clues.accumulator();
		final ARI folderSource = ari(Path.of("Example Board"));
		final ARI fileSource = ari(Path.of("Example Board", "retro.md"));

		clues.foundBy("BracketishFinder");
		clues.addAll(Clues.accumulator()
				.add(Clue.of("bus", "AGP").from(folderSource), ClueLocation.in(folderName, 14, 9)).clues());
		clues.foundBy("MarkdownishFinder");

		final DuplicateClueException failure = assertThrows(DuplicateClueException.class, () -> clues.addAll(Clues
				.accumulator().add(Clue.of("bus", "PCI").from(fileSource), ClueLocation.in(document, 4, 3)).clues()));

		assertEquals("Duplicate clue key 'bus'. One artifact may contain only one clue for a key. "
				+ "First values: [AGP], duplicate values: [PCI].\n"
				+ "  The first clue was observed by BracketishFinder from " + folderSource
				+ ", line 1, column 15.\n    Example Board [bus AGP]\n                  ^^^^^^^^^\n"
				+ "  The duplicate clue was observed by MarkdownishFinder from " + fileSource
				+ ", line 2, column 1.\n    bus: PCI\n    ^^^", failure.getMessage());
	}

	/**
	 * Positions stop at the cache boundary. A retrieved artifact has no folder
	 * name or document left to point into, so diagnostics must not differ
	 * depending on whether an archive came from a crawl or the repository.
	 */
	@Test
	void dropsPositionsWhenTheCluesBecomeAnArtifact() {
		final Clues located = Clues.accumulator().add(Clue.of("bus", "AGP"), ClueLocation.in("Board [bus AGP]", 7, 7))
				.clues();
		assertTrue(located.hasLocations());

		assertFalse(new Artifact(located).clues().hasLocations());
	}

	@Test
	void reportsTheFinderAndSourceForAFailureTheFinderDidNotLocate() {
		final BracketishFinder finder = new BracketishFinder();
		final DuplicateClueException duplicate = assertThrows(DuplicateClueException.class,
				() -> finder.find(folder("Example Board")));
		final ARI source = ari(Path.of("Example Board"));

		final ClueFindingException failure = ClueFindingException.from(finder.getClass().getSimpleName(), source,
				duplicate);

		assertEquals(source, failure.source().orElseThrow());
		assertEquals("BracketishFinder", failure.finder().orElseThrow());
		assertInstanceOf(DuplicateClueException.class, failure.getCause());
		assertTrue(failure.getMessage().startsWith(source + ": BracketishFinder failed while finding clues."),
				failure.getMessage());
	}

	@Test
	void reportsTheAuthoritativeArchiveResource() {
		final ARI source = ari(Path.of("Graphics Cards", "Example Board"));
		final ClueFindingException located = ClueFindingException.from("BracketishFinder", source,
				new DuplicateClueException("Duplicate clue key 'bus'."));

		assertEquals(source, located.source().orElseThrow());
		assertEquals(ARCHIVE_ID, located.archiveId().orElseThrow());
		assertEquals(source + ": BracketishFinder failed while finding clues.\nDuplicate clue key 'bus'.",
				located.getMessage());
	}

	@Test
	void keepsAPositionAFinderReportedItself() {
		final ClueLocation location = ClueLocation.in("Board [!]", 6, 3);
		final ARI source = ari(Path.of("Board"));

		final ClueFindingException reported = ClueFindingException.from("BracketishFinder", source,
				new ClueFindingException("Reserved key.", location));

		assertEquals(location, reported.location().orElseThrow());
		assertEquals(source + ":1:7: BracketishFinder failed while finding clues.\nReserved key.\n  Board [!]\n"
				+ "        ^^^", reported.getMessage());
	}

	/**
	 * Emits two clues claiming one key so the framework has something to
	 * report.
	 */
	private static final class BracketishFinder implements ClueFinder {

		@Override
		public Clues find(final ArchiveFolderView folder) {
			return Clues.of(Clue.of("bus", "ISA"), Clue.of("bus", "PCI"));
		}
	}

	private static final class MarkdownishFinder implements ClueFinder {

		@Override
		public Clues find(final ArchiveFolderView folder) {
			return Clues.none();
		}
	}

	private static ArchiveFolderView folder(final String name) {
		return new ArchiveFolderView() {

			@Override
			public ARI ari() {
				return ClueDiagnosticsTest.ari(Path.of(name));
			}

			@Override
			public String name() {
				return name;
			}

			@Override
			public List<ArchiveFolderView> folders() {
				return List.of();
			}

			@Override
			public List<ArchiveFileView> files() {
				return List.of();
			}
		};
	}

	private static ARI ari(final Path path) {
		return ARI.of("test_collection", ARCHIVE_ID, path);
	}
}
