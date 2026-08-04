package com.retrocrawler.core.archive;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.ArchiveNode;

class InMemoryRepositoryTest {

	@Test
	void retrievesStowedArchive() {
		final Repository repository = new InMemoryRepository();
		final Archive archive = archive("test_archive");

		repository.stowaway(archive);

		assertSame(archive, repository.retrieve(ArchiveId.of("test_archive")).orElseThrow());
	}

	@Test
	void returnsEmptyForMissingArchive() {
		final Repository repository = new InMemoryRepository();

		assertTrue(repository.retrieve(ArchiveId.of("missing")).isEmpty());
	}

	@Test
	void replacesExistingArchiveWithSameId() {
		final Repository repository = new InMemoryRepository();
		final Archive first = archive("replace_me");
		final Archive replacement = archive("replace_me");

		repository.stowaway(first);
		repository.stowaway(replacement);

		assertSame(replacement, repository.retrieve(ArchiveId.of("replace_me")).orElseThrow());
	}

	@Test
	void keepsArchivesWithDifferentIds() {
		final Repository repository = new InMemoryRepository();
		final Archive first = archive("first");
		final Archive second = archive("second");

		repository.stowaway(first);
		repository.stowaway(second);

		assertSame(first, repository.retrieve(first.id()).orElseThrow());
		assertSame(second, repository.retrieve(second.id()).orElseThrow());
	}

	private static Archive archive(final String id) {
		return Archive.of(ArchiveId.of(id), Path.of(id), new ArchiveNode(id, null, null));
	}

}
