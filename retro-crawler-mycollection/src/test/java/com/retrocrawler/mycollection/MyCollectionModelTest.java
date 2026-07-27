package com.retrocrawler.mycollection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.DuplicateRetroIdException;
import com.retrocrawler.core.Model;
import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.ArchiveRoots;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.util.Monitor;
import com.retrocrawler.model.hardware.ExpansionBus;
import com.retrocrawler.model.identifier.ISBN;
import com.retrocrawler.model.identifier.MacAddress;
import com.retrocrawler.model.identifier.TheRetroWebId;
import com.retrocrawler.model.measurement.DataCapacity;
import com.retrocrawler.mycollection.catalog.FloppyImageId;
import com.retrocrawler.mycollection.catalog.RetroId;
import com.retrocrawler.mycollection.gear.GraphicsCard;
import com.retrocrawler.mycollection.gear.MyGear;
import com.retrocrawler.mycollection.gear.MysteryGear;
import com.retrocrawler.mycollection.memory.RamSet;

class MyCollectionModelTest {

	private static final Monitor SILENT_MONITOR = new Monitor(message -> {
		// No progress output required in tests.
	});

	@TempDir
	private Path archiveRoot;

	@Test
	void resolvesTheMinimalCollectionModelFromSyntheticFolders() throws IOException {
		Files.createDirectories(archiveRoot.resolve("Grouping folder without tags"));
		Files.createDirectories(archiveRoot.resolve("Mystery object [200001]"));
		Files.createDirectories(archiveRoot.resolve("Unnumbered object [PCI]"));
		Files.createDirectories(archiveRoot.resolve("Known card [AGP] [200002]"));
		Files.createDirectories(archiveRoot.resolve("Serial only [SN 200003]"));

		final List<MyGear> gear = crawler().crawlGear(SILENT_MONITOR, true, MyGear.class);

		assertEquals(4, gear.size());

		final MyGear identifiedMystery = gear(gear, "Mystery object [200001]");
		assertInstanceOf(MysteryGear.class, identifiedMystery);
		assertEquals(Optional.of(new RetroId(200001)), identifiedMystery.getRetroId());

		final MyGear unnumbered = gear(gear, "Unnumbered object [PCI]");
		assertInstanceOf(MysteryGear.class, unnumbered);
		assertEquals(Optional.empty(), unnumbered.getRetroId());
		assertEquals(Optional.of(ExpansionBus.PCI), unnumbered.getBus());

		final MyGear graphicsCard = gear(gear, "Known card [AGP] [200002]");
		assertInstanceOf(GraphicsCard.class, graphicsCard);
		assertEquals(Optional.of(new RetroId(200002)), graphicsCard.getRetroId());
		assertEquals(Optional.of(ExpansionBus.AGP), graphicsCard.getBus());

		final MyGear serialOnly = gear(gear, "Serial only [SN 200003]");
		assertInstanceOf(MysteryGear.class, serialOnly);
		assertEquals(Optional.empty(), serialOnly.getRetroId());
		assertEquals(Optional.of("200003"), serialOnly.getSerialNumber());
		assertFalse(serialOnly.getAttributes().containsKey(AttributeNames.SERIAL_NUMBER));
	}

	@Test
	void resolvesTheRetroWebAndRamSetFacts() throws IOException {
		Files.createDirectories(archiveRoot.resolve("Memory set [32MB] [Set 2 x 16MB] [TRW 10510]"));

		final MyGear gear = gear(crawler().crawlGear(SILENT_MONITOR, true, MyGear.class),
				"Memory set [32MB] [Set 2 x 16MB] [TRW 10510]");

		final DataCapacity expectedCapacity = new DataCapacity(java.math.BigDecimal.valueOf(32),
				DataCapacity.Unit.MB);
		final RamSet expectedSet = new RamSet(2,
				new DataCapacity(java.math.BigDecimal.valueOf(16), DataCapacity.Unit.MB));
		assertEquals(Optional.of(expectedCapacity), gear.getCapacity());
		assertEquals(Optional.of(expectedSet), gear.getRamSet());
		assertTrue(expectedSet.totalCapacity().sameSizeAs(expectedCapacity));
		assertEquals(Optional.of(new TheRetroWebId(10510)), gear.getTheRetroWebId());
	}

	@Test
	void permitsTheSameRetroWebEntryForDifferentPhysicalGear() throws IOException {
		Files.createDirectories(archiveRoot.resolve("First board [200010] [trw 10510]"));
		Files.createDirectories(archiveRoot.resolve("Second board [200011] [TRW 10510]"));

		final List<MyGear> gear = crawler().crawlGear(SILENT_MONITOR, true, MyGear.class);

		assertEquals(2, gear.size());
		assertTrue(gear.stream()
				.allMatch(value -> value.getTheRetroWebId().equals(Optional.of(new TheRetroWebId(10510)))));
	}

	@Test
	void resolvesSharedIdentifierTypesThroughCollectionKeys() throws IOException {
		Files.createDirectories(archiveRoot.resolve(
				"Network manual [MAC 00-00-C0-0D-66-AB] [ISBN 978-0-306-40615-7]"));

		final MyGear gear = gear(crawler().crawlGear(SILENT_MONITOR, true, MyGear.class),
				"Network manual [MAC 00-00-C0-0D-66-AB] [ISBN 978-0-306-40615-7]");

		assertEquals(Optional.of(new MacAddress("00:00:C0:0D:66:AB")), gear.getMacAddress());
		assertEquals(Optional.of(new ISBN("9780306406157")), gear.getIsbn());
	}

	@Test
	void rejectsDuplicateRetroIdsWithTheirSourcePaths() throws IOException {
		final Path first = Files.createDirectories(archiveRoot.resolve("Duplicate A [200004]"));
		final Path second = Files.createDirectories(archiveRoot.resolve("Duplicate B [200004]"));

		final DuplicateRetroIdException failure = assertThrows(DuplicateRetroIdException.class,
				() -> crawler().crawlGear(SILENT_MONITOR, true, MyGear.class));

		final List<String> paths = failure.getDuplicates().get(new RetroId(200004));
		assertEquals(2, paths.size());
		assertTrue(paths.contains(first.toString()));
		assertTrue(paths.contains(second.toString()));
	}

	@Test
	void resolvesFileDerivedFactsFromTheCurrentGearFolder() throws IOException {
		final Path folder = Files.createDirectories(archiveRoot.resolve("Documented object [200005]"));
		final String markdown = "# Notes\n\nThis description belongs to the containing gear.\n";
		Files.writeString(folder.resolve("retro.md"), markdown);
		final Path angled = Files.createFile(folder.resolve("angled.jpeg"));
		final Path front = Files.createFile(folder.resolve("front.jpeg"));
		final Path back = Files.createFile(folder.resolve("back.jpeg"));
		final Path floppy = Files.createFile(folder.resolve("FD-0007 System disk.img"));

		final MyGear gear = gear(crawler().crawlGear(SILENT_MONITOR, true, MyGear.class),
				"Documented object [200005]");

		assertEquals(Optional.of(markdown), gear.getDescription());
		assertEquals(Optional.of(angled.toString()), gear.getAngledImage());
		assertEquals(Optional.of(front.toString()), gear.getFrontImage());
		assertEquals(Optional.of(back.toString()), gear.getBackImage());
		assertEquals(Set.of(new FloppyImageId("FD-0007")), gear.getFloppyImageIds());
		assertEquals(Set.of(floppy.toString()), gear.getFloppyImages());
	}

	@Test
	void letsAFileClueEstablishAnOtherwiseUntaggedArtifact() throws IOException {
		final Path folder = Files.createDirectories(archiveRoot.resolve("Notes only"));
		Files.writeString(folder.resolve("retro.md"), "A note is an intentional description.");

		final List<MyGear> gear = crawler().crawlGear(SILENT_MONITOR, true, MyGear.class);

		assertEquals(1, gear.size());
		assertInstanceOf(MysteryGear.class, gear.getFirst());
		assertEquals(Optional.of("A note is an intentional description."), gear.getFirst().getDescription());
	}

	@Test
	void ignoresAFolderContainingOnlyABookmark() throws IOException {
		final Path folder = Files.createDirectories(archiveRoot.resolve("Acquisition source"));
		Files.createFile(folder.resolve("listing.webloc"));

		assertTrue(crawler().crawlGear(SILENT_MONITOR, true, MyGear.class).isEmpty());
	}

	@Test
	void keepsContradictoryFolderAndPropertiesValuesAsAnUnresolvedClue() throws IOException {
		final Path folder = Files.createDirectories(archiveRoot.resolve("Conflicting object [AGP] [200006]"));
		Files.writeString(folder.resolve("retro.properties"), "bus=PCI\n");

		final MyGear gear = gear(crawler().crawlGear(SILENT_MONITOR, true, MyGear.class),
				"Conflicting object [AGP] [200006]");

		assertInstanceOf(MysteryGear.class, gear);
		assertEquals(Optional.empty(), gear.getBus());
		assertEquals(Set.of("AGP", "PCI"),
				assertInstanceOf(Clue.class, gear.getAttributes().get(AttributeNames.BUS)).getValue());
	}

	@Test
	void collapsesCorroboratingFolderAndPropertiesValues() throws IOException {
		final Path folder = Files.createDirectories(archiveRoot.resolve("Corroborated card [AGP] [200007]"));
		Files.writeString(folder.resolve("retro.properties"), "bus=AGP\n");

		final MyGear gear = gear(crawler().crawlGear(SILENT_MONITOR, true, MyGear.class),
				"Corroborated card [AGP] [200007]");

		assertInstanceOf(GraphicsCard.class, gear);
		assertEquals(Optional.of(ExpansionBus.AGP), gear.getBus());
	}

	private RetroCrawler crawler() {
		final Model model = Model.from("com.retrocrawler.mycollection", ArchiveRoots.from(archiveRoot));
		return RetroCrawler.builder().model(model).repository(new MemoryRepository()).build();
	}

	private static MyGear gear(final List<MyGear> gear, final String folderName) {
		return gear.stream().filter(candidate -> folderName.equals(candidate.getFolderName())).findFirst().orElseThrow();
	}

	private static final class MemoryRepository implements Repository {

		private Archive archive;

		@Override
		public void stowaway(final Archive archive) {
			this.archive = archive;
		}

		@Override
		public Optional<Archive> retrieve(final ArchiveId id) {
			return Optional.ofNullable(archive);
		}
	}
}
