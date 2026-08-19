package com.retrocrawler.core.archive.clues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class ClueDiagnosticsTest {

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

		clues.observing(ClueSource.folderView(new BracketishFinder()));
		clues.addAll(Clues.accumulator().add(Clue.of("bus", "AGP"), ClueLocation.in(folderName, 14, 9)).clues());
		clues.observing(ClueSource.fileContent("retro.md", new MarkdownishFinder()));

		final DuplicateClueException failure = assertThrows(DuplicateClueException.class, () -> clues
				.addAll(Clues.accumulator().add(Clue.of("bus", "PCI"), ClueLocation.in(document, 4, 3)).clues()));

		assertEquals("""
			Duplicate clue key 'bus'. One artifact may contain only one clue for a key. \
			First values: [AGP], duplicate values: [PCI].
			  The first clue was observed where BracketishFinder read the archive folder, line 1, column 15.
			    Example Board [bus AGP]
			                  ^^^^^^^^^
			  The duplicate clue was observed where MarkdownishFinder read the file content of \
			'retro.md', line 2, column 1.
			    bus: PCI
			    ^^^""", failure.getMessage());
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

		final ClueFindingException failure = ClueFindingException.from(ClueSource.folderView(finder), duplicate)
				.in(Path.of("Example Board"));

		assertEquals(ClueSourceKind.FOLDER_VIEW, failure.source().orElseThrow().kind());
		assertEquals(BracketishFinder.class, failure.source().orElseThrow().finder());
		assertInstanceOf(DuplicateClueException.class, failure.getCause());
		assertTrue(failure.getMessage().startsWith("Example Board: BracketishFinder read the archive folder."),
				failure.getMessage());
	}

	@Test
	void completesTheReportWithTheArchiveRelativeFolder() {
		final ClueSource source = ClueSource.folderView(new BracketishFinder());
		final ClueFindingException reported = ClueFindingException.from(source,
				new DuplicateClueException("Duplicate clue key 'bus'."));

		final ClueFindingException located = reported.in(Path.of("Graphics Cards", "Example Board"));

		assertEquals(Path.of("Graphics Cards", "Example Board"), located.folder().orElseThrow());
		assertEquals("""
			Graphics Cards/Example Board: BracketishFinder read the archive folder.
			Duplicate clue key 'bus'.""", located.getMessage());
	}

	@Test
	void keepsAPositionAFinderReportedItself() {
		final ClueLocation location = ClueLocation.in("Board [!]", 6, 3);
		final ClueSource source = ClueSource.folderView(new BracketishFinder());

		final ClueFindingException reported = ClueFindingException
				.from(source, new ClueFindingException("Reserved key.", location)).in(Path.of("Board"));

		assertEquals(location, reported.location().orElseThrow());
		assertEquals("""
			Board:1:7: BracketishFinder read the archive folder.
			Reserved key.
			  Board [!]
			        ^^^""", reported.getMessage());
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
}
