package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.annotation.RetroCollection;
import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.FolderNameClueFinder;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.progress.Progressor;
import com.retrocrawler.core.util.RetroAttribute;

class CacheModelEvolutionTest {

	private static final ArchiveId ARCHIVE_ID = ArchiveId.of("test_archive");

	private static final Progressor SILENT_PROGRESSOR = new Progressor();

	@TempDir
	private Path archiveRoot;

	@Test
	void reinterpretsRawCachedCluesWhenTheModelGainsAFactKey() throws IOException {
		Files.createDirectories(archiveRoot.resolve("serial-pending"));
		final MemoryRepository repository = new MemoryRepository();

		final Model initialModel = Model.from(Set.of(TestArchive.class, InitialGear.class));
		final RetroCrawler initialCrawler = RetroCrawler.builder().model(initialModel).repository(repository)
				.archive(ArchiveDescriptor.of(ARCHIVE_ID, archiveRoot)).build();
		initialCrawler.crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), InitialGear.class);

		final Archive cachedArchive = repository.archive;
		assertEquals(1, repository.stowawayCount);
		assertRawAnonymousSerialMarker(cachedArchive);

		final Model evolvedModel = Model.from(Set.of(TestArchive.class, EvolvedGear.class));
		final RetroCrawler evolvedCrawler = RetroCrawler.builder().model(evolvedModel).repository(repository)
				.archive(ArchiveDescriptor.of(ARCHIVE_ID, archiveRoot)).build();
		final List<EvolvedGear> gear = evolvedCrawler.crawlAllGear(SILENT_PROGRESSOR, ReindexScope.none(),
				EvolvedGear.class);

		assertEquals(1, gear.size());
		assertNull(gear.getFirst().serialNumber);
		final Clue missingSerial = assertInstanceOf(Clue.class, gear.getFirst().attributes.get("sn"));
		assertTrue(missingSerial.isMissingValue());
		assertEquals(1, repository.stowawayCount);
		assertSame(cachedArchive, repository.archive);
		assertRawAnonymousSerialMarker(repository.archive);
	}

	private static void assertRawAnonymousSerialMarker(final Archive archive) {
		final Artifact artifact = archive.root().children().getFirst().artifact();
		assertTrue(artifact.clues().stream().anyMatch(clue -> clue.isAnonymous() && clue.value().equals(Set.of("SN"))));
		assertTrue(artifact.clues().stream().noneMatch(clue -> "sn".equals(clue.key())));
	}

	@RetroCollection(id = "cache_model_evolution")
	@RetroClues(fromFolderName = TestClueFinder.class)
	public static final class TestArchive {

		private TestArchive() {
		}
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class InitialGear {

		@RetroAnyAttribute
		private final Map<String, RetroAttribute> attributes = new HashMap<>();

		public InitialGear() {
		}
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class EvolvedGear {

		@RetroFact(key = "sn")
		private String serialNumber;

		@RetroAnyAttribute
		private final Map<String, RetroAttribute> attributes = new HashMap<>();

		public EvolvedGear() {
		}
	}

	public static final class TestClueFinder implements FolderNameClueFinder {

		@Override
		public Set<Clue> find(final String folderName) {
			return "serial-pending".equals(folderName) ? Set.of(Clue.of("SN")) : Set.of();
		}
	}

	private static final class MemoryRepository implements Repository {

		private Archive archive;

		private int stowawayCount;

		@Override
		public void stowaway(final Archive archive) {
			this.archive = archive;
			stowawayCount++;
		}

		@Override
		public Optional<Archive> retrieve(final ArchiveId id) {
			return Optional.ofNullable(archive);
		}
	}
}
