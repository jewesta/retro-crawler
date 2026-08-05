package com.retrocrawler.mycollection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.DuplicateRetroIdException;
import com.retrocrawler.core.Model;
import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.JsonFileRepository;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.filter.IgnoreDotPaths;
import com.retrocrawler.core.archive.filter.IgnoreLinuxSystemPaths;
import com.retrocrawler.core.archive.filter.IgnoreMacSystemPaths;
import com.retrocrawler.core.archive.filter.IgnoreQNAPSystemPaths;
import com.retrocrawler.core.archive.filter.IgnoreSynologySystemPaths;
import com.retrocrawler.core.archive.filter.IgnoreWindowsSystemPaths;
import com.retrocrawler.core.progress.Progressor;
import com.retrocrawler.model.appearance.Color;
import com.retrocrawler.model.commerce.Money;
import com.retrocrawler.model.condition.DamageKind;
import com.retrocrawler.model.condition.FunctionalCondition;
import com.retrocrawler.model.condition.ItemCondition;
import com.retrocrawler.model.hardware.ChipDesignation;
import com.retrocrawler.model.hardware.ComputerFormFactor;
import com.retrocrawler.model.hardware.ExpansionBus;
import com.retrocrawler.model.hardware.MemoryAccessTime;
import com.retrocrawler.model.hardware.MemoryFeature;
import com.retrocrawler.model.hardware.MemoryFormFactor;
import com.retrocrawler.model.hardware.MemoryStandard;
import com.retrocrawler.model.hardware.VideoConnector;
import com.retrocrawler.model.identifier.MacAddress;
import com.retrocrawler.model.identifier.NintendoGameBoyCartridgeCode;
import com.retrocrawler.model.identifier.NintendoGameBoyPlatform;
import com.retrocrawler.model.identifier.PlayStationPortableDiscId;
import com.retrocrawler.model.identifier.PlayStationPortableDiscPrefix;
import com.retrocrawler.model.identifier.SegaGameGearCartridgeCode;
import com.retrocrawler.model.identifier.TheRetroWebCategory;
import com.retrocrawler.model.identifier.TheRetroWebId;
import com.retrocrawler.model.identifier.TheRetroWebReference;
import com.retrocrawler.model.locale.LanguageCode;
import com.retrocrawler.model.locale.RegionCode;
import com.retrocrawler.model.measurement.CapacitySet;
import com.retrocrawler.model.measurement.DataCapacity;
import com.retrocrawler.model.measurement.Length;
import com.retrocrawler.model.measurement.Length.Unit;
import com.retrocrawler.model.measurement.Power;
import com.retrocrawler.model.measurement.TrackDensity;
import com.retrocrawler.model.packaging.PackagingOrigin;
import com.retrocrawler.model.packaging.SealState;
import com.retrocrawler.model.software.Version;
import com.retrocrawler.model.storage.FloppyDiskFormFactor;
import com.retrocrawler.model.storage.FloppyDiskFormat;
import com.retrocrawler.model.storage.FloppyDiskFormat.Density;
import com.retrocrawler.model.storage.FloppyDiskFormat.Sides;
import com.retrocrawler.model.storage.HardDiskDriveFormFactor;
import com.retrocrawler.model.temporal.DateMarking;
import com.retrocrawler.model.temporal.YearWeek;
import com.retrocrawler.mycollection.catalog.Destiny;
import com.retrocrawler.mycollection.catalog.DocumentId;
import com.retrocrawler.mycollection.catalog.FloppyImageId;
import com.retrocrawler.mycollection.catalog.RetroId;
import com.retrocrawler.mycollection.catalog.Tested;
import com.retrocrawler.mycollection.gear.Diskette;
import com.retrocrawler.mycollection.gear.GraphicsCard;
import com.retrocrawler.mycollection.gear.HardDiskDrive;
import com.retrocrawler.mycollection.gear.MemoryModule;
import com.retrocrawler.mycollection.gear.Motherboard;
import com.retrocrawler.mycollection.gear.MyGear;
import com.retrocrawler.mycollection.gear.MysteryGear;
import com.retrocrawler.mycollection.gear.PowerSupply;
import com.retrocrawler.mycollection.references.TheRetroWebReferences;

import de.creativecouple.validation.isbn.ISBN;

class MyCollectionModelTest {

	private static final ArchiveId ARCHIVE_ID = ArchiveId.of("my_collection");

	private static final Progressor SILENT_PROGRESSOR = new Progressor();

	@TempDir
	private Path archiveRoot;

	@Test
	void configuresAllBundledArchivePathFilters() {
		final Model model = Model.from("com.retrocrawler.mycollection");

		assertEquals(
				List.of(IgnoreDotPaths.class, IgnoreWindowsSystemPaths.class, IgnoreMacSystemPaths.class,
						IgnoreLinuxSystemPaths.class, IgnoreQNAPSystemPaths.class, IgnoreSynologySystemPaths.class),
				model.pathFilters().stream().map(Object::getClass).toList());
	}

	@Test
	void resolvesTheMinimalCollectionModelFromSyntheticFolders() throws IOException {
		Files.createDirectories(archiveRoot.resolve("Grouping folder without tags"));
		Files.createDirectories(archiveRoot.resolve("Mystery object [200001]"));
		Files.createDirectories(archiveRoot.resolve("Unnumbered object [PCI]"));
		Files.createDirectories(archiveRoot.resolve("Known card [AGP] [VGA] [200002] [TRW 10510]"));
		Files.createDirectories(archiveRoot.resolve("Serial only [SN 200003]"));

		final List<MyGear> gear = crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class);

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
		final TheRetroWebReference expectedReference = new TheRetroWebReference(TheRetroWebCategory.EXPANSION_CARD,
				new TheRetroWebId(10510));
		assertEquals(Optional.of(expectedReference), new TheRetroWebReferences().referenceFor(graphicsCard));
		assertEquals("https://theretroweb.com/expansioncards/10510", expectedReference.lookupUri().toString());

		final MyGear serialOnly = gear(gear, "Serial only [SN 200003]");
		assertInstanceOf(MysteryGear.class, serialOnly);
		assertEquals(Optional.empty(), serialOnly.getRetroId());
		assertEquals(Optional.of("200003"), serialOnly.getSerialNumber());
		assertFalse(serialOnly.getAttributes().containsKey(AttributeNames.SERIAL_NUMBER));
	}

	@Test
	void resolvesTheRetroWebAndCapacitySetFacts() throws IOException {
		Files.createDirectories(archiveRoot.resolve("Memory set [32MB] [Set 2 x 16MB] [TRW 10510]"));

		final MyGear gear = gear(crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class),
				"Memory set [32MB] [Set 2 x 16MB] [TRW 10510]");

		final DataCapacity expectedCapacity = new DataCapacity(java.math.BigDecimal.valueOf(32), DataCapacity.Unit.MB);
		final CapacitySet expectedSet = new CapacitySet(2,
				new DataCapacity(java.math.BigDecimal.valueOf(16), DataCapacity.Unit.MB));
		assertEquals(Optional.of(expectedCapacity), gear.getCapacity());
		assertEquals(Optional.of(expectedSet), gear.getCapacitySet());
		assertTrue(expectedSet.totalCapacity().sameSizeAs(expectedCapacity));
		assertEquals(Optional.of(new TheRetroWebId(10510)), gear.getTheRetroWebId());
		assertEquals(Optional.empty(), new TheRetroWebReferences().referenceFor(gear));
	}

	@Test
	void resolvesMinedGearSignaturesFromTheirOwnTags() throws IOException {
		Files.createDirectories(archiveRoot.resolve("Moved board [ATX] [ISA, PCI, AGP] [TRW 10510] [200020]"));
		Files.createDirectories(archiveRoot.resolve("Moved old memory [SIMM72] [8MB] [70ns] [EDO] [200021]"));
		Files.createDirectories(archiveRoot.resolve("Moved DDR memory [PC3200] [512MB] [200022]"));
		Files.createDirectories(archiveRoot.resolve("Moved supply [ATX] [300W] [200023]"));

		final List<MyGear> gear = crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class);

		final MyGear board = gear(gear, "Moved board [ATX] [ISA, PCI, AGP] [TRW 10510] [200020]");
		assertInstanceOf(Motherboard.class, board);
		assertEquals(Set.of(ExpansionBus.ISA, ExpansionBus.PCI, ExpansionBus.AGP), board.getExpansionBuses());
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
	void recognizesDiskettesOnlyFromCombinedLocalTechnicalEvidence() throws IOException {
		Files.createDirectories(archiveRoot.resolve("First disk [3,5\"] [48TPI] [DS] [HD] [200024]"));
		Files.createDirectories(archiveRoot.resolve("Second disk [96TPI] [2S-HD] [200025]"));
		Files.createDirectories(archiveRoot.resolve("Drive with track density only [96TPI] [200026]"));

		final List<MyGear> gear = crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class);

		final Diskette first = assertInstanceOf(Diskette.class,
				gear(gear, "First disk [3,5\"] [48TPI] [DS] [HD] [200024]"));
		assertEquals(Optional.of(FloppyDiskFormFactor.INCH_3_5), first.getFormFactor());
		assertEquals(Optional.empty(), first.getLength());
		assertEquals(Set.of(new TrackDensity(48)), first.getTrackDensities());
		assertEquals(Set.of(FloppyDiskFormat.sides(Sides.DOUBLE), FloppyDiskFormat.density(Density.HIGH)),
				first.getFloppyDiskFormats());

		final Diskette second = assertInstanceOf(Diskette.class, gear(gear, "Second disk [96TPI] [2S-HD] [200025]"));
		assertEquals(Set.of(new TrackDensity(96)), second.getTrackDensities());
		assertEquals(Set.of(FloppyDiskFormat.of(Sides.DOUBLE, Density.HIGH)), second.getFloppyDiskFormats());

		assertInstanceOf(MysteryGear.class, gear(gear, "Drive with track density only [96TPI] [200026]"));
	}

	@Test
	void treatsDocumentIdsAsReusableReferencesRatherThanGearIdentity() throws IOException {
		Files.createDirectories(archiveRoot.resolve("First scanned manual [101534] [200030]"));
		Files.createDirectories(archiveRoot.resolve("Second scanned manual [101534] [200031]"));

		final List<MyGear> gear = crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class);

		assertEquals(2, gear.size());
		assertTrue(gear.stream().allMatch(value -> value.getDocumentIds().equals(Set.of(new DocumentId(101534)))));
	}

	@Test
	void resolvesMeasurementsGenericallyUntilTheGearTypeSuppliesTheirMeaning() throws IOException {
		Files.createDirectories(
				archiveRoot.resolve("Floppy release [3,5\"] [1,44MB] [1989] [v5.0] [gg 2449] [101534]"));
		Files.createDirectories(archiveRoot.resolve("Hard drive [HDD] [2,5″]"));
		Files.createDirectories(archiveRoot.resolve("Measured object [19″]"));
		Files.createDirectories(archiveRoot.resolve("Colored object [schwarz] [weiß, pink]"));

		final List<MyGear> gear = crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class);
		final MyGear floppy = gear(gear, "Floppy release [3,5\"] [1,44MB] [1989] [v5.0] [gg 2449] [101534]");
		assertInstanceOf(MysteryGear.class, floppy);
		assertEquals(Optional.of(new Length(new BigDecimal("3.5"), Unit.INCH)), floppy.getLength());
		assertEquals(Optional.of(new DataCapacity(new BigDecimal("1.44"), DataCapacity.Unit.MB)), floppy.getCapacity());
		assertEquals(Set.of(Year.of(1989)), floppy.getYears());
		assertEquals(Optional.of(new Version("v5.0")), floppy.getVersion());
		assertEquals(Set.of(new SegaGameGearCartridgeCode("2449")), floppy.getSegaGameGearCartridgeCodes());
		assertEquals(Set.of(new DocumentId(101534)), floppy.getDocumentIds());

		final HardDiskDrive hardDrive = assertInstanceOf(HardDiskDrive.class, gear(gear, "Hard drive [HDD] [2,5″]"));
		assertEquals(Optional.of(HardDiskDriveFormFactor.INCH_2_5), hardDrive.getFormFactor());
		assertEquals(Optional.empty(), hardDrive.getLength());

		final MyGear measured = gear(gear, "Measured object [19″]");
		assertEquals(Optional.of(new Length(BigDecimal.valueOf(19), Unit.INCH)), measured.getLength());
		assertEquals(Optional.empty(), measured.getScreenSize());

		final MyGear colored = gear(gear, "Colored object [schwarz] [weiß, pink]");
		assertEquals(Set.of(Color.BLACK, Color.WHITE, Color.PINK), colored.getColors());
	}

	@Test
	void resolvesOnlyExplicitlyKeyedChipDesignationsAndAllowsSeveral() throws IOException {
		Files.createDirectories(archiveRoot.resolve("Controller [IC RC42-A] [ic Example Semiconductor 7]"));
		Files.createDirectories(archiveRoot.resolve("Anonymous observation [RC42-A]"));

		final List<MyGear> gear = crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class);

		assertEquals(Set.of(new ChipDesignation("RC42-A"), new ChipDesignation("Example Semiconductor 7")),
				gear(gear, "Controller [IC RC42-A] [ic Example Semiconductor 7]").getChipDesignations());
		assertTrue(gear(gear, "Anonymous observation [RC42-A]").getChipDesignations().isEmpty());
	}

	@Test
	void resolvesDateMarkingsWithoutConflatingMonthAndWeekPrecision() throws IOException {
		Files.createDirectories(archiveRoot.resolve("Year observation [1994]"));
		Files.createDirectories(archiveRoot.resolve("Month observation [1994-05]"));
		Files.createDirectories(archiveRoot.resolve("Week observation [1994-KW05]"));
		Files.createDirectories(archiveRoot.resolve("Exact date [1994-05-12]"));

		final List<MyGear> gear = crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class);

		final MyGear year = gear(gear, "Year observation [1994]");
		assertEquals(Optional.of(DateMarking.of(Year.of(1994))), year.getDateMarking());
		assertEquals(Set.of(Year.of(1994)), year.getYears());

		final MyGear month = gear(gear, "Month observation [1994-05]");
		assertEquals(Optional.of(DateMarking.of(YearMonth.of(1994, 5))), month.getDateMarking());
		assertTrue(month.getYears().isEmpty());

		final MyGear week = gear(gear, "Week observation [1994-KW05]");
		assertEquals(Optional.of(DateMarking.of(new YearWeek(1994, 5))), week.getDateMarking());
		assertTrue(week.getYears().isEmpty());

		assertEquals(Optional.of(DateMarking.of(LocalDate.of(1994, 5, 12))),
				gear(gear, "Exact date [1994-05-12]").getDateMarking());
	}

	@Test
	void resolvesIndependentConditionHealthAndDamageFacts() throws IOException {
		Files.createDirectories(archiveRoot.resolve("New object [Neu]"));
		Files.createDirectories(archiveRoot.resolve("Used object [gebraucht]"));
		Files.createDirectories(archiveRoot.resolve("Refurbished object [refurbished]"));
		Files.createDirectories(archiveRoot.resolve("Damaged object [beschädigt]"));
		Files.createDirectories(archiveRoot.resolve("Faulty object [defekt] [Akkuschaden]"));
		Files.createDirectories(archiveRoot.resolve("Partially faulty object [teildefekt] [Bruch]"));

		final List<MyGear> gear = crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class);

		assertEquals(Optional.of(ItemCondition.NEW), gear(gear, "New object [Neu]").getCondition());
		assertEquals(Optional.of(ItemCondition.USED), gear(gear, "Used object [gebraucht]").getCondition());
		assertEquals(Optional.of(ItemCondition.REFURBISHED),
				gear(gear, "Refurbished object [refurbished]").getCondition());
		assertEquals(Optional.of(ItemCondition.DAMAGED), gear(gear, "Damaged object [beschädigt]").getCondition());

		final MyGear faulty = gear(gear, "Faulty object [defekt] [Akkuschaden]");
		assertEquals(Optional.of(FunctionalCondition.DEFECTIVE), faulty.getHealth());
		assertEquals(Set.of(DamageKind.BATTERY_DAMAGE), faulty.getDamageKinds());

		final MyGear partiallyFaulty = gear(gear, "Partially faulty object [teildefekt] [Bruch]");
		assertEquals(Optional.of(FunctionalCondition.PARTIALLY_DEFECTIVE), partiallyFaulty.getHealth());
		assertEquals(Set.of(DamageKind.BREAKAGE), partiallyFaulty.getDamageKinds());
	}

	@Test
	void resolvesPackagingOriginAndSealStateAsIndependentFacts() throws IOException {
		Files.createDirectories(archiveRoot.resolve("Original package [OVP]"));
		Files.createDirectories(archiveRoot.resolve("Sealed package [sealed]"));
		Files.createDirectories(archiveRoot.resolve("German sealed package [versiegelt]"));
		Files.createDirectories(archiveRoot.resolve("Opened package [geöffnet]"));
		Files.createDirectories(archiveRoot.resolve("Original sealed package [OVP] [sealed]"));

		final List<MyGear> gear = crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class);

		final MyGear original = gear(gear, "Original package [OVP]");
		assertEquals(Optional.of(PackagingOrigin.ORIGINAL), original.getPackagingOrigin());
		assertTrue(original.hasOriginalPackaging());
		assertTrue(original.getSealState().isEmpty());
		assertFalse(original.isSealed());
		assertFalse(original.isExplicitlyOpened());

		final MyGear sealed = gear(gear, "Sealed package [sealed]");
		assertEquals(Optional.of(SealState.SEALED), sealed.getSealState());
		assertTrue(sealed.isSealed());
		assertFalse(sealed.hasOriginalPackaging());

		assertEquals(Optional.of(SealState.SEALED), gear(gear, "German sealed package [versiegelt]").getSealState());

		final MyGear opened = gear(gear, "Opened package [geöffnet]");
		assertEquals(Optional.of(SealState.OPENED), opened.getSealState());
		assertTrue(opened.isExplicitlyOpened());
		assertFalse(opened.isSealed());

		final MyGear originalSealed = gear(gear, "Original sealed package [OVP] [sealed]");
		assertEquals(Optional.of(PackagingOrigin.ORIGINAL), originalSealed.getPackagingOrigin());
		assertEquals(Optional.of(SealState.SEALED), originalSealed.getSealState());
		assertTrue(originalSealed.hasOriginalPackaging());
		assertTrue(originalSealed.isSealed());
	}

	@Test
	void permitsTheSameRetroWebEntryForDifferentPhysicalGear() throws IOException {
		Files.createDirectories(archiveRoot.resolve("First board [200010] [trw 10510]"));
		Files.createDirectories(archiveRoot.resolve("Second board [200011] [TRW 10510]"));

		final List<MyGear> gear = crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class);

		assertEquals(2, gear.size());
		assertTrue(gear.stream()
				.allMatch(value -> value.getTheRetroWebId().equals(Optional.of(new TheRetroWebId(10510)))));
	}

	@Test
	void resolvesSharedIdentifierTypesThroughCollectionKeys() throws IOException {
		Files.createDirectories(archiveRoot.resolve("Network manual [MAC 00-00-C0-0D-66-AB] [ISBN 978-0-306-40615-7]"));

		final MyGear gear = gear(crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class),
				"Network manual [MAC 00-00-C0-0D-66-AB] [ISBN 978-0-306-40615-7]");

		assertEquals(Optional.of(new MacAddress("00:00:C0:0D:66:AB")), gear.getMacAddress());
		assertEquals(Optional.of(ISBN.valueOf("978-0-306-40615-7")), gear.getIsbn());
	}

	@Test
	void resolvesGameBoyCartridgeCodesAndPspDiscIdsFromAnonymousClues() throws IOException {
		Files.createDirectories(archiveRoot.resolve("Game Boy release [DMG-A1-EUR] [200037]"));
		Files.createDirectories(archiveRoot.resolve("PSP release [ULES-01234] [200038]"));

		final List<MyGear> gear = crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class);

		final MyGear gameBoy = gear(gear, "Game Boy release [DMG-A1-EUR] [200037]");
		assertEquals(Set.of(new NintendoGameBoyCartridgeCode(NintendoGameBoyPlatform.GAME_BOY, "A1", "EUR")),
				gameBoy.getNintendoGameBoyCartridgeCodes());

		final MyGear psp = gear(gear, "PSP release [ULES-01234] [200038]");
		assertEquals(Set.of(new PlayStationPortableDiscId(PlayStationPortableDiscPrefix.ULES, "01234")),
				psp.getPlayStationPortableDiscIds());
	}

	@Test
	void resolvesUnambiguousLocaleCodesAndPreservesAmbiguousAnonymousCodes() throws IOException {
		Files.createDirectories(archiveRoot.resolve("Unambiguous locale [EN] [US] [200032]"));
		Files.createDirectories(archiveRoot.resolve("European release [EU] [200035]"));
		Files.createDirectories(archiveRoot.resolve("Rejected language alias [language EU] [200036]"));
		Files.createDirectories(archiveRoot.resolve("Ambiguous locale [DE] [200033]"));
		Files.createDirectories(archiveRoot.resolve("Explicit locale [language DE] [region DE] [200034]"));

		final List<MyGear> gear = crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class);

		final MyGear unambiguous = gear(gear, "Unambiguous locale [EN] [US] [200032]");
		assertEquals(Set.of(new LanguageCode("en")), unambiguous.getLanguages());
		assertEquals(Set.of(new RegionCode("US")), unambiguous.getRegions());

		final MyGear european = gear(gear, "European release [EU] [200035]");
		assertTrue(european.getLanguages().isEmpty());
		assertEquals(Set.of(new RegionCode("EUR")), european.getRegions());

		final MyGear rejectedLanguage = gear(gear, "Rejected language alias [language EU] [200036]");
		assertTrue(rejectedLanguage.getLanguages().isEmpty());
		assertTrue(rejectedLanguage.getAttributes().get(AttributeNames.LANGUAGE) instanceof Clue);

		final MyGear ambiguous = gear(gear, "Ambiguous locale [DE] [200033]");
		assertTrue(ambiguous.getLanguages().isEmpty());
		assertTrue(ambiguous.getRegions().isEmpty());
		assertTrue(ambiguous.getAttributes().values().stream().filter(Clue.class::isInstance).map(Clue.class::cast)
				.anyMatch(clue -> clue.isAnonymous() && clue.value().equals(Set.of("DE"))));

		final MyGear explicit = gear(gear, "Explicit locale [language DE] [region DE] [200034]");
		assertEquals(Set.of(new LanguageCode("de")), explicit.getLanguages());
		assertEquals(Set.of(new RegionCode("DE")), explicit.getRegions());
	}

	@Test
	void rejectsDuplicateRetroIdsWithTheirSourceAris() throws IOException {
		Files.createDirectories(archiveRoot.resolve("Duplicate A [200004]"));
		Files.createDirectories(archiveRoot.resolve("Duplicate B [200004]"));

		final DuplicateRetroIdException failure = assertThrows(DuplicateRetroIdException.class,
				() -> crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class));

		final List<String> paths = failure.duplicates().get(new RetroId(200004));
		assertEquals(2, paths.size());
		assertTrue(paths.contains(
				ARI.of("my_collection", ArchiveId.of("my_collection"), Path.of("Duplicate A [200004]")).toString()));
		assertTrue(paths.contains(
				ARI.of("my_collection", ArchiveId.of("my_collection"), Path.of("Duplicate B [200004]")).toString()));
	}

	@Test
	void resolvesFileDerivedFactsFromTheCurrentGearFolder() throws IOException {
		final Path folder = Files.createDirectories(archiveRoot.resolve("Documented object [200005]"));
		final String markdown = """
			---
			price: 120 EUR
			source: kleinanzeigen.de
			fcc: TEST-FCC-123
			health: defekt
			tested: post
			---
			This description belongs to the containing gear.
			""";
		Files.writeString(folder.resolve("retro.md"), markdown);
		final Path angled = Files.createFile(folder.resolve("angled.jpeg"));
		final Path front = Files.createFile(folder.resolve("front.jpeg"));
		final Path back = Files.createFile(folder.resolve("back.jpeg"));
		final Path floppy = Files.createFile(folder.resolve("FD-0007 System disk.img"));

		final MyGear gear = gear(crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class),
				"Documented object [200005]");

		assertEquals(Optional.of("This description belongs to the containing gear."), gear.getDescription());
		assertEquals(Optional.of(new Money(new BigDecimal("120"), Currency.getInstance("EUR"))), gear.getPrice());
		assertEquals(Optional.empty(), gear.getLotPrice());
		assertEquals(Optional.of("kleinanzeigen.de"), gear.getSource());
		assertEquals(Optional.of("TEST-FCC-123"), gear.getFccId());
		assertEquals(Optional.of(FunctionalCondition.DEFECTIVE), gear.getHealth());
		assertEquals(Optional.of(Tested.POST), gear.getTested());
		assertEquals(Optional.of(angled), gear.getAngledImage());
		assertEquals(Optional.of(front), gear.getFrontImage());
		assertEquals(Optional.of(back), gear.getBackImage());
		assertEquals(Set.of(new FloppyImageId("FD-0007")), gear.getFloppyImageIds());
		assertEquals(Set.of(floppy), gear.getFloppyImages());
	}

	@Test
	void rebindsCachedRelativeFileCluesToANewArchiveRoot() throws IOException {
		final Path nasRoot = Files.createDirectory(archiveRoot.resolve("nas-root"));
		final Path gearFolder = Files.createDirectory(nasRoot.resolve("Portable object [200006]"));
		Files.createFile(gearFolder.resolve("front.jpeg"));
		final JsonFileRepository repository = new JsonFileRepository(archiveRoot.resolve("repository"));

		crawler(nasRoot, repository).crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class);

		final Archive stored = repository.retrieve(ARCHIVE_ID).orElseThrow();
		final Clue storedImage = stored.root().children().getFirst().artifact().clues().stream()
				.filter(clue -> AttributeNames.IMAGE_FRONT.equals(clue.key())).findFirst().orElseThrow();
		assertEquals(Set.of(Path.of("Portable object [200006]", "front.jpeg").toString()), storedImage.value());

		final Path desktopRoot = Files.move(nasRoot, archiveRoot.resolve("desktop-root"));
		final MyGear rebound = gear(
				crawler(desktopRoot, repository).crawlAllGear(SILENT_PROGRESSOR, ReindexScope.none(), MyGear.class),
				"Portable object [200006]");

		assertEquals(Optional.of(desktopRoot.resolve("Portable object [200006]").resolve("front.jpeg")),
				rebound.getFrontImage());
	}

	@Test
	void resolvesAnOpenSourceLotMembershipAndLotPriceWithoutPretendingItIsAnItemPrice() throws IOException {
		final Path folder = Files.createDirectories(archiveRoot.resolve("Lot member [200009]"));
		Files.writeString(folder.resolve("retro.md"), """
			---
			source: future-market.example
			lot: 200009, 200010
			lot-price: 100
			---
			""");

		final MyGear gear = gear(crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class),
				"Lot member [200009]");

		assertEquals(Optional.of("future-market.example"), gear.getSource());
		assertEquals(Optional.of(new Money(new BigDecimal("100"), Currency.getInstance("EUR"))), gear.getLotPrice());
		assertEquals(Set.of(new RetroId(200009), new RetroId(200010)), gear.getLot());
		assertEquals(Optional.empty(), gear.getPrice());
	}

	@Test
	void letsAFileClueEstablishAnOtherwiseUntaggedArtifact() throws IOException {
		final Path folder = Files.createDirectories(archiveRoot.resolve("Notes only"));
		Files.writeString(folder.resolve("retro.md"), "A note is an intentional description.");

		final List<MyGear> gear = crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class);

		assertEquals(1, gear.size());
		assertInstanceOf(MysteryGear.class, gear.getFirst());
		assertEquals(Optional.of("A note is an intentional description."), gear.getFirst().getDescription());
	}

	@Test
	void preservesAKnownKeyWithoutAValueAsAnArtifactClue() throws IOException {
		Files.createDirectories(archiveRoot.resolve("Serial pending [SN]"));
		Files.createDirectories(archiveRoot.resolve("Actually empty []"));

		final List<MyGear> gear = crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class);

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

		assertTrue(crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class).isEmpty());
	}

	@Test
	void combinesExpansionBusesAcrossFolderAndMarkdownClues() throws IOException {
		final Path folder = Files.createDirectories(archiveRoot.resolve("Conflicting object [AGP] [200006]"));
		Files.writeString(folder.resolve("retro.md"), "---\nbus: PCI\n---\n");

		final MyGear gear = gear(crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class),
				"Conflicting object [AGP] [200006]");

		assertInstanceOf(MysteryGear.class, gear);
		assertEquals(Set.of(ExpansionBus.AGP, ExpansionBus.PCI), gear.getExpansionBuses());
		assertFalse(gear.getAttributes().containsKey(AttributeNames.BUS));
	}

	@Test
	void collapsesCorroboratingFolderAndMarkdownValues() throws IOException {
		final Path folder = Files.createDirectories(archiveRoot.resolve("Corroborated card [AGP] [VGA] [200007]"));
		Files.writeString(folder.resolve("retro.md"), "---\nbus: AGP\n---\n");

		final MyGear gear = gear(crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class),
				"Corroborated card [AGP] [VGA] [200007]");

		assertInstanceOf(GraphicsCard.class, gear);
		assertEquals(Set.of(ExpansionBus.AGP), gear.getExpansionBuses());
	}

	@Test
	void resolvesGermanDestinyTagsWithoutKeepingLegacySynonyms() throws IOException {
		Files.createDirectories(archiveRoot.resolve("[verschenkt] Passed-on object [200008]"));

		final MyGear gear = gear(crawler().crawlAllGear(SILENT_PROGRESSOR, ReindexScope.all(), MyGear.class),
				"[verschenkt] Passed-on object [200008]");

		assertEquals(Optional.of(Destiny.VERSCHENKT), gear.getDestiny());
	}

	private RetroCrawler crawler() {
		return crawler(archiveRoot, new MemoryRepository());
	}

	private static RetroCrawler crawler(final Path root, final Repository repository) {
		final Model model = Model.from("com.retrocrawler.mycollection");
		return RetroCrawler.builder().model(model).repository(repository)
				.archive(ArchiveDescriptor.of(ARCHIVE_ID, root)).build();
	}

	private static MyGear gear(final List<MyGear> gear, final String folderName) {
		return gear.stream().filter(candidate -> folderName.equals(candidate.getFolderName())).findFirst()
				.orElseThrow();
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
