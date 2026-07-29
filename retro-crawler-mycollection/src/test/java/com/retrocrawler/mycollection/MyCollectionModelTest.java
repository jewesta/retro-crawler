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
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.progress.Progressor;
import com.retrocrawler.model.hardware.ComputerFormFactor;
import com.retrocrawler.model.hardware.ExpansionBus;
import com.retrocrawler.model.hardware.MemoryAccessTime;
import com.retrocrawler.model.hardware.MemoryFeature;
import com.retrocrawler.model.hardware.MemoryFormFactor;
import com.retrocrawler.model.hardware.MemoryStandard;
import com.retrocrawler.model.hardware.VideoConnector;
import com.retrocrawler.model.identifier.ISBN;
import com.retrocrawler.model.identifier.MacAddress;
import com.retrocrawler.model.identifier.TheRetroWebCategory;
import com.retrocrawler.model.identifier.TheRetroWebId;
import com.retrocrawler.model.identifier.TheRetroWebReference;
import com.retrocrawler.model.measurement.DataCapacity;
import com.retrocrawler.model.measurement.Power;
import com.retrocrawler.mycollection.catalog.FloppyImageId;
import com.retrocrawler.mycollection.catalog.RetroId;
import com.retrocrawler.mycollection.catalog.ScanId;
import com.retrocrawler.mycollection.gear.GraphicsCard;
import com.retrocrawler.mycollection.gear.MemoryModule;
import com.retrocrawler.mycollection.gear.Motherboard;
import com.retrocrawler.mycollection.gear.MyGear;
import com.retrocrawler.mycollection.gear.MysteryGear;
import com.retrocrawler.mycollection.gear.PowerSupply;
import com.retrocrawler.mycollection.memory.RamSet;
import com.retrocrawler.mycollection.references.TheRetroWebReferences;

class MyCollectionModelTest {

	private static final Progressor SILENT_PROGRESSOR = new Progressor();

	@TempDir
	private Path archiveRoot;

	@Test
	void resolvesTheMinimalCollectionModelFromSyntheticFolders() throws IOException {
		Files.createDirectories(archiveRoot.resolve("Grouping folder without tags"));
		Files.createDirectories(archiveRoot.resolve("Mystery object [200001]"));
		Files.createDirectories(archiveRoot.resolve("Unnumbered object [PCI]"));
		Files.createDirectories(archiveRoot.resolve("Known card [AGP] [VGA] [200002] [TRW 10510]"));
		Files.createDirectories(archiveRoot.resolve("Serial only [SN 200003]"));

		final List<MyGear> gear = crawler().crawlGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class);

		assertEquals(4, gear.size());

		final MyGear identifiedMystery = gear(gear, "Mystery object [200001]");
		assertInstanceOf(MysteryGear.class, identifiedMystery);
		assertEquals(Optional.of(new RetroId(200001)), identifiedMystery.getRetroId());

		final MyGear unnumbered = gear(gear, "Unnumbered object [PCI]");
		assertInstanceOf(MysteryGear.class, unnumbered);
		assertEquals(Optional.empty(), unnumbered.getRetroId());
		assertEquals(Set.of(ExpansionBus.PCI), unnumbered.getExpansionBuses());

		final MyGear graphicsCard = gear(gear, "Known card [AGP] [VGA] [200002] [TRW 10510]");
		assertInstanceOf(GraphicsCard.class, graphicsCard);
		assertEquals(Optional.of(new RetroId(200002)), graphicsCard.getRetroId());
		assertEquals(Set.of(ExpansionBus.AGP), graphicsCard.getExpansionBuses());
		assertEquals(Set.of(VideoConnector.VGA), graphicsCard.getVideoConnectors());
		final TheRetroWebReference expectedReference = new TheRetroWebReference(
				TheRetroWebCategory.EXPANSION_CARD, new TheRetroWebId(10510));
		assertEquals(Optional.of(expectedReference),
				new TheRetroWebReferences().referenceFor(graphicsCard));
		assertEquals("https://theretroweb.com/expansioncards/10510",
				expectedReference.lookupUri().toString());

		final MyGear serialOnly = gear(gear, "Serial only [SN 200003]");
		assertInstanceOf(MysteryGear.class, serialOnly);
		assertEquals(Optional.empty(), serialOnly.getRetroId());
		assertEquals(Optional.of("200003"), serialOnly.getSerialNumber());
		assertFalse(serialOnly.getAttributes().containsKey(AttributeNames.SERIAL_NUMBER));
	}

	@Test
	void resolvesTheRetroWebAndRamSetFacts() throws IOException {
		Files.createDirectories(archiveRoot.resolve("Memory set [32MB] [Set 2 x 16MB] [TRW 10510]"));

		final MyGear gear = gear(crawler().crawlGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class),
				"Memory set [32MB] [Set 2 x 16MB] [TRW 10510]");

		final DataCapacity expectedCapacity = new DataCapacity(java.math.BigDecimal.valueOf(32),
				DataCapacity.Unit.MB);
		final RamSet expectedSet = new RamSet(2,
				new DataCapacity(java.math.BigDecimal.valueOf(16), DataCapacity.Unit.MB));
		assertEquals(Optional.of(expectedCapacity), gear.getCapacity());
		assertEquals(Optional.of(expectedSet), gear.getRamSet());
		assertTrue(expectedSet.totalCapacity().sameSizeAs(expectedCapacity));
		assertEquals(Optional.of(new TheRetroWebId(10510)), gear.getTheRetroWebId());
		assertEquals(Optional.empty(), new TheRetroWebReferences().referenceFor(gear));
	}

	@Test
	void resolvesMinedGearSignaturesFromTheirOwnTags() throws IOException {
		Files.createDirectories(
				archiveRoot.resolve("Moved board [ATX] [ISA, PCI, AGP] [TRW 10510] [200020]"));
		Files.createDirectories(
				archiveRoot.resolve("Moved old memory [SIMM72] [8MB] [70ns] [EDO] [200021]"));
		Files.createDirectories(archiveRoot.resolve("Moved DDR memory [PC3200] [512MB] [200022]"));
		Files.createDirectories(archiveRoot.resolve("Moved supply [ATX] [300W] [200023]"));

		final List<MyGear> gear = crawler().crawlGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class);

		final MyGear board = gear(gear, "Moved board [ATX] [ISA, PCI, AGP] [TRW 10510] [200020]");
		assertInstanceOf(Motherboard.class, board);
		assertEquals(Set.of(ExpansionBus.ISA, ExpansionBus.PCI, ExpansionBus.AGP),
				board.getExpansionBuses());
		assertEquals(Set.of(ComputerFormFactor.ATX), board.getComputerFormFactors());
		assertEquals(Optional.of(TheRetroWebCategory.MOTHERBOARD.reference(new TheRetroWebId(10510))),
				new TheRetroWebReferences().referenceFor(board));

		final MyGear oldMemory = gear(gear, "Moved old memory [SIMM72] [8MB] [70ns] [EDO] [200021]");
		assertInstanceOf(MemoryModule.class, oldMemory);
		assertEquals(Set.of(MemoryFormFactor.SIMM_72_PIN), oldMemory.getMemoryFormFactors());
		assertEquals(Set.of(new MemoryAccessTime(70)), oldMemory.getMemoryAccessTimes());
		assertEquals(Set.of(MemoryFeature.EXTENDED_DATA_OUT), oldMemory.getMemoryFeatures());

		final MyGear ddrMemory = gear(gear, "Moved DDR memory [PC3200] [512MB] [200022]");
		assertInstanceOf(MemoryModule.class, ddrMemory);
		assertEquals(Set.of(MemoryStandard.PC_3200), ddrMemory.getMemoryStandards());

		final MyGear supply = gear(gear, "Moved supply [ATX] [300W] [200023]");
		assertInstanceOf(PowerSupply.class, supply);
		assertEquals(Optional.of(new Power(java.math.BigDecimal.valueOf(300))), supply.getPower());
	}

	@Test
	void treatsScanIdsAsReusableReferencesRatherThanGearIdentity() throws IOException {
		Files.createDirectories(archiveRoot.resolve("First scanned manual [101534] [200030]"));
		Files.createDirectories(archiveRoot.resolve("Second scanned manual [101534] [200031]"));

		final List<MyGear> gear = crawler().crawlGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class);

		assertEquals(2, gear.size());
		assertTrue(gear.stream().allMatch(value -> value.getScanIds().equals(Set.of(new ScanId(101534)))));
	}

	@Test
	void permitsTheSameRetroWebEntryForDifferentPhysicalGear() throws IOException {
		Files.createDirectories(archiveRoot.resolve("First board [200010] [trw 10510]"));
		Files.createDirectories(archiveRoot.resolve("Second board [200011] [TRW 10510]"));

		final List<MyGear> gear = crawler().crawlGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class);

		assertEquals(2, gear.size());
		assertTrue(gear.stream()
				.allMatch(value -> value.getTheRetroWebId().equals(Optional.of(new TheRetroWebId(10510)))));
	}

	@Test
	void resolvesSharedIdentifierTypesThroughCollectionKeys() throws IOException {
		Files.createDirectories(archiveRoot.resolve(
				"Network manual [MAC 00-00-C0-0D-66-AB] [ISBN 978-0-306-40615-7]"));

		final MyGear gear = gear(crawler().crawlGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class),
				"Network manual [MAC 00-00-C0-0D-66-AB] [ISBN 978-0-306-40615-7]");

		assertEquals(Optional.of(new MacAddress("00:00:C0:0D:66:AB")), gear.getMacAddress());
		assertEquals(Optional.of(new ISBN("9780306406157")), gear.getIsbn());
	}

	@Test
	void rejectsDuplicateRetroIdsWithTheirSourcePaths() throws IOException {
		final Path first = Files.createDirectories(archiveRoot.resolve("Duplicate A [200004]"));
		final Path second = Files.createDirectories(archiveRoot.resolve("Duplicate B [200004]"));

		final DuplicateRetroIdException failure = assertThrows(DuplicateRetroIdException.class,
				() -> crawler().crawlGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class));

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

		final MyGear gear = gear(crawler().crawlGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class),
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

		final List<MyGear> gear = crawler().crawlGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class);

		assertEquals(1, gear.size());
		assertInstanceOf(MysteryGear.class, gear.getFirst());
		assertEquals(Optional.of("A note is an intentional description."), gear.getFirst().getDescription());
	}

	@Test
	void preservesAKnownKeyWithoutAValueAsAnArtifactClue() throws IOException {
		Files.createDirectories(archiveRoot.resolve("Serial pending [SN]"));
		Files.createDirectories(archiveRoot.resolve("Actually empty []"));

		final List<MyGear> gear = crawler().crawlGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class);

		assertEquals(1, gear.size());
		final MyGear serialPending = gear.getFirst();
		assertInstanceOf(MysteryGear.class, serialPending);
		assertEquals(Optional.of("Serial pending"), serialPending.getTitle());
		assertEquals(Optional.empty(), serialPending.getSerialNumber());
		final Clue missingSerial = assertInstanceOf(Clue.class,
				serialPending.getAttributes().get(AttributeNames.SERIAL_NUMBER));
		assertTrue(missingSerial.isMissingValue());
	}

	@Test
	void ignoresAFolderContainingOnlyABookmark() throws IOException {
		final Path folder = Files.createDirectories(archiveRoot.resolve("Acquisition source"));
		Files.createFile(folder.resolve("listing.webloc"));

		assertTrue(crawler().crawlGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class).isEmpty());
	}

	@Test
	void combinesExpansionBusesAcrossFolderAndPropertiesClues() throws IOException {
		final Path folder = Files.createDirectories(archiveRoot.resolve("Conflicting object [AGP] [200006]"));
		Files.writeString(folder.resolve("retro.properties"), "bus=PCI\n");

		final MyGear gear = gear(crawler().crawlGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class),
				"Conflicting object [AGP] [200006]");

		assertInstanceOf(MysteryGear.class, gear);
		assertEquals(Set.of(ExpansionBus.AGP, ExpansionBus.PCI), gear.getExpansionBuses());
		assertFalse(gear.getAttributes().containsKey(AttributeNames.BUS));
	}

	@Test
	void collapsesCorroboratingFolderAndPropertiesValues() throws IOException {
		final Path folder = Files.createDirectories(
				archiveRoot.resolve("Corroborated card [AGP] [VGA] [200007]"));
		Files.writeString(folder.resolve("retro.properties"), "bus=AGP\n");

		final MyGear gear = gear(crawler().crawlGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class),
				"Corroborated card [AGP] [VGA] [200007]");

		assertInstanceOf(GraphicsCard.class, gear);
		assertEquals(Set.of(ExpansionBus.AGP), gear.getExpansionBuses());
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
