package com.retrocrawler.demo.clues;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.clues.ArchiveFileView;
import com.retrocrawler.core.archive.clues.ArchiveFolderView;
import com.retrocrawler.core.archive.clues.Clues;

class SquareBracketsClueFinderTest {

	@Test
	void normalizesEveryNamedKeyWithoutKnowingItsVocabulary() {
		final Clues clues = new SquareBracketsClueFinder()
				.find(folder("Manual [ISBN 978-0-306-40615-7] [Alias one, two]"));

		assertEquals(Set.of("978-0-306-40615-7"), clues.get("isbn").orElseThrow().value());
		assertEquals(Set.of("one", "two"), clues.get("alias").orElseThrow().value());
		assertEquals(
				List.of(ARI.of("test_collection", ArchiveId.of("test_archive"),
						Path.of("Manual [ISBN 978-0-306-40615-7] [Alias one, two]"))),
				clues.get("isbn").orElseThrow().sources());
	}

	private static ArchiveFolderView folder(final String name) {
		return new ArchiveFolderView() {

			@Override
			public ARI ari() {
				return ARI.of("test_collection", ArchiveId.of("test_archive"), Path.of(name));
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
}
