package com.retrocrawler.mycollection.gear;

import java.nio.file.Path;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.archive.clues.InternalClueKeys;
import com.retrocrawler.core.gear.parser.StringParser;
import com.retrocrawler.core.util.RetroAttribute;
import com.retrocrawler.model.appearance.Color;
import com.retrocrawler.model.commerce.Money;
import com.retrocrawler.model.hardware.ComputerFormFactor;
import com.retrocrawler.model.hardware.ComputerFormFactorParser;
import com.retrocrawler.model.hardware.ExpansionBus;
import com.retrocrawler.model.hardware.ExpansionBusParser;
import com.retrocrawler.model.hardware.MemoryAccessTime;
import com.retrocrawler.model.hardware.MemoryAccessTimeParser;
import com.retrocrawler.model.hardware.MemoryFeature;
import com.retrocrawler.model.hardware.MemoryFeatureParser;
import com.retrocrawler.model.hardware.MemoryFormFactor;
import com.retrocrawler.model.hardware.MemoryFormFactorParser;
import com.retrocrawler.model.hardware.MemoryStandard;
import com.retrocrawler.model.hardware.MemoryStandardParser;
import com.retrocrawler.model.hardware.VideoConnector;
import com.retrocrawler.model.hardware.VideoConnectorParser;
import com.retrocrawler.model.identifier.ISBNParser;
import com.retrocrawler.model.identifier.MacAddress;
import com.retrocrawler.model.identifier.MacAddressParser;
import com.retrocrawler.model.identifier.NintendoGameBoyCartridgeCode;
import com.retrocrawler.model.identifier.NintendoGameBoyCartridgeCodeParser;
import com.retrocrawler.model.identifier.PlayStationPortableDiscId;
import com.retrocrawler.model.identifier.PlayStationPortableDiscIdParser;
import com.retrocrawler.model.identifier.SegaGameGearCartridgeCode;
import com.retrocrawler.model.identifier.SegaGameGearCartridgeCodeParser;
import com.retrocrawler.model.identifier.TheRetroWebId;
import com.retrocrawler.model.identifier.TheRetroWebIdParser;
import com.retrocrawler.model.locale.LanguageCode;
import com.retrocrawler.model.locale.RegionCode;
import com.retrocrawler.model.measurement.DataCapacity;
import com.retrocrawler.model.measurement.DataCapacityParser;
import com.retrocrawler.model.measurement.Power;
import com.retrocrawler.model.measurement.PowerParser;
import com.retrocrawler.model.measurement.ScreenSize;
import com.retrocrawler.model.software.Version;
import com.retrocrawler.model.software.VersionParser;
import com.retrocrawler.model.storage.FloppyDiskFormFactor;
import com.retrocrawler.model.storage.FloppyDiskFormFactorParser;
import com.retrocrawler.model.storage.HardDiskDriveFormFactor;
import com.retrocrawler.model.temporal.YearParser;
import com.retrocrawler.mycollection.AttributeNames;
import com.retrocrawler.mycollection.catalog.Destiny;
import com.retrocrawler.mycollection.catalog.DocumentId;
import com.retrocrawler.mycollection.catalog.FloppyImageId;
import com.retrocrawler.mycollection.catalog.RetroId;
import com.retrocrawler.mycollection.catalog.Tested;
import com.retrocrawler.mycollection.facts.CollectionColorParser;
import com.retrocrawler.mycollection.facts.CollectionHardDiskDriveFormFactorParser;
import com.retrocrawler.mycollection.facts.CollectionLanguageCodeParser;
import com.retrocrawler.mycollection.facts.CollectionRegionCodeParser;
import com.retrocrawler.mycollection.facts.CollectionScreenSizeParser;
import com.retrocrawler.mycollection.facts.DestinyParser;
import com.retrocrawler.mycollection.facts.DocumentIdParser;
import com.retrocrawler.mycollection.facts.FloppyImageIdParser;
import com.retrocrawler.mycollection.facts.MoneyParser;
import com.retrocrawler.mycollection.facts.RamSetParser;
import com.retrocrawler.mycollection.facts.RetroIdParser;
import com.retrocrawler.mycollection.facts.TestedParser;
import com.retrocrawler.mycollection.memory.RamSet;

import de.creativecouple.validation.isbn.ISBN;

public abstract class MyGear {

	@RetroFact(key = InternalClueKeys.FOLDER, optional = false)
	private String folderName;

	@com.retrocrawler.core.annotation.RetroId
	@RetroFact(key = AttributeNames.RETRO_ID, parser = RetroIdParser.class, strict = false, optional = true)
	private RetroId retroId;

	@RetroFact(key = AttributeNames.BUS, parser = ExpansionBusParser.class, strict = false, optional = true)
	private Set<ExpansionBus> expansionBuses = Set.of();

	@RetroFact(key = AttributeNames.CAPACITY, parser = DataCapacityParser.class, strict = false, optional = true)
	private DataCapacity capacity;

	@RetroFact(key = AttributeNames.COLOR, parser = CollectionColorParser.class, strict = false, optional = true)
	private Set<Color> colors = Set.of();

	@RetroFact(key = AttributeNames.FLOPPY_DISK_FORM_FACTOR, parser = FloppyDiskFormFactorParser.class,
			strict = false, optional = true)
	private Set<FloppyDiskFormFactor> floppyDiskFormFactors = Set.of();

	@RetroFact(key = AttributeNames.HARD_DISK_DRIVE_FORM_FACTOR,
			parser = CollectionHardDiskDriveFormFactorParser.class, strict = false, optional = true)
	private Set<HardDiskDriveFormFactor> hardDiskDriveFormFactors = Set.of();

	@RetroFact(key = AttributeNames.MEMORY_ACCESS_TIME, parser = MemoryAccessTimeParser.class, strict = false,
			optional = true)
	private Set<MemoryAccessTime> memoryAccessTimes = Set.of();

	@RetroFact(key = AttributeNames.MEMORY_FEATURE, parser = MemoryFeatureParser.class, strict = false, optional = true)
	private Set<MemoryFeature> memoryFeatures = Set.of();

	@RetroFact(key = AttributeNames.MEMORY_FORM_FACTOR, parser = MemoryFormFactorParser.class, strict = false,
			optional = true)
	private Set<MemoryFormFactor> memoryFormFactors = Set.of();

	@RetroFact(key = AttributeNames.MEMORY_STANDARD, parser = MemoryStandardParser.class, strict = false,
			optional = true)
	private Set<MemoryStandard> memoryStandards = Set.of();

	@RetroFact(key = AttributeNames.COMPUTER_FORM_FACTOR, parser = ComputerFormFactorParser.class,
			strict = false, optional = true)
	private Set<ComputerFormFactor> computerFormFactors = Set.of();

	@RetroFact(key = AttributeNames.POWER, parser = PowerParser.class, strict = false, optional = true)
	private Power power;

	@RetroFact(key = AttributeNames.SCREEN_SIZE, parser = CollectionScreenSizeParser.class, strict = false,
			optional = true)
	private Set<ScreenSize> screenSizes = Set.of();

	@RetroFact(key = AttributeNames.VERSION, parser = VersionParser.class, strict = false, optional = true)
	private Version version;

	@RetroFact(key = AttributeNames.YEAR, parser = YearParser.class, strict = false, optional = true)
	private Set<Year> years = Set.of();

	@RetroFact(key = AttributeNames.TITLE, optional = true)
	private String title;

	@RetroFact(key = AttributeNames.DESC, optional = true)
	private String description;

	@RetroFact(key = AttributeNames.DESTINY, parser = DestinyParser.class, strict = false, optional = true)
	private Destiny destiny;

	@RetroFact(key = AttributeNames.FCC_ID, optional = true)
	private String fccId;

	@RetroFact(key = AttributeNames.HEALTH, optional = true)
	private String health;

	@RetroFact(key = AttributeNames.PRICE, parser = MoneyParser.class, optional = true)
	private Money price;

	@RetroFact(key = AttributeNames.LOT, parser = RetroIdParser.class, optional = true)
	private Set<RetroId> lot = Set.of();

	@RetroFact(key = AttributeNames.LOT_PRICE, parser = MoneyParser.class, optional = true)
	private Money lotPrice;

	@RetroFact(key = AttributeNames.SOURCE, optional = true)
	private String source;

	@RetroFact(key = AttributeNames.TESTED, parser = TestedParser.class, optional = true)
	private Tested tested;

	@RetroFact(key = AttributeNames.IMAGE_ANGLED, optional = true)
	private Path angledImage;

	@RetroFact(key = AttributeNames.IMAGE_FRONT, optional = true)
	private Path frontImage;

	@RetroFact(key = AttributeNames.IMAGE_BACK, optional = true)
	private Path backImage;

	@RetroFact(key = AttributeNames.ISBN, parser = ISBNParser.class, optional = true)
	private ISBN isbn;

	@RetroFact(key = AttributeNames.LANGUAGE, parser = CollectionLanguageCodeParser.class, strict = false,
			optional = true)
	private Set<LanguageCode> languages = Set.of();

	@RetroFact(key = AttributeNames.REGION, parser = CollectionRegionCodeParser.class, strict = false,
			optional = true)
	private Set<RegionCode> regions = Set.of();

	@RetroFact(key = AttributeNames.MAC_ADDRESS, parser = MacAddressParser.class, optional = true)
	private MacAddress macAddress;

	@RetroFact(key = AttributeNames.NINTENDO_GAME_BOY_CARTRIDGE_CODE,
			parser = NintendoGameBoyCartridgeCodeParser.class, strict = false, optional = true)
	private Set<NintendoGameBoyCartridgeCode> nintendoGameBoyCartridgeCodes = Set.of();

	@RetroFact(key = AttributeNames.PSP_DISC_ID, parser = PlayStationPortableDiscIdParser.class,
			strict = false, optional = true)
	private Set<PlayStationPortableDiscId> playStationPortableDiscIds = Set.of();

	@RetroFact(key = AttributeNames.SEGA_GAME_GEAR_CARTRIDGE_CODE,
			parser = SegaGameGearCartridgeCodeParser.class, optional = true)
	private Set<SegaGameGearCartridgeCode> segaGameGearCartridgeCodes = Set.of();

	@RetroFact(key = AttributeNames.SERIAL_NUMBER, parser = StringParser.class, optional = true)
	private String serialNumber;

	@RetroFact(key = AttributeNames.FLOPPY_IMAGE_ID, parser = FloppyImageIdParser.class, optional = true)
	private Set<FloppyImageId> floppyImageIds = Set.of();

	@RetroFact(key = AttributeNames.FLOPPY_IMAGES, optional = true)
	private Set<Path> floppyImages = Set.of();

	@RetroFact(key = AttributeNames.RAM_SET, parser = RamSetParser.class, optional = true)
	private RamSet ramSet;

	@RetroFact(key = AttributeNames.DOCUMENT, parser = DocumentIdParser.class, strict = false, optional = true)
	private Set<DocumentId> documentIds = Set.of();

	@RetroFact(key = AttributeNames.THE_RETRO_WEB_ID, parser = TheRetroWebIdParser.class, optional = true)
	private TheRetroWebId theRetroWebId;

	@RetroFact(key = AttributeNames.VIDEO_CONNECTOR, parser = VideoConnectorParser.class, strict = false,
			optional = true)
	private Set<VideoConnector> videoConnectors = Set.of();

	@RetroAnyAttribute
	private final Map<String, RetroAttribute> attributes = new HashMap<>();

	public String getFolderName() {
		return folderName;
	}

	public Optional<RetroId> getRetroId() {
		return Optional.ofNullable(retroId);
	}

	public Set<ExpansionBus> getExpansionBuses() {
		return Set.copyOf(expansionBuses);
	}

	public Optional<DataCapacity> getCapacity() {
		return Optional.ofNullable(capacity);
	}

	public Set<Color> getColors() {
		return Set.copyOf(colors);
	}

	public Set<FloppyDiskFormFactor> getFloppyDiskFormFactors() {
		return Set.copyOf(floppyDiskFormFactors);
	}

	public Set<HardDiskDriveFormFactor> getHardDiskDriveFormFactors() {
		return Set.copyOf(hardDiskDriveFormFactors);
	}

	public Set<MemoryAccessTime> getMemoryAccessTimes() {
		return Set.copyOf(memoryAccessTimes);
	}

	public Set<MemoryFeature> getMemoryFeatures() {
		return Set.copyOf(memoryFeatures);
	}

	public Set<MemoryFormFactor> getMemoryFormFactors() {
		return Set.copyOf(memoryFormFactors);
	}

	public Set<MemoryStandard> getMemoryStandards() {
		return Set.copyOf(memoryStandards);
	}

	public Set<ComputerFormFactor> getComputerFormFactors() {
		return Set.copyOf(computerFormFactors);
	}

	public Optional<Power> getPower() {
		return Optional.ofNullable(power);
	}

	public Set<ScreenSize> getScreenSizes() {
		return Set.copyOf(screenSizes);
	}

	public Optional<Version> getVersion() {
		return Optional.ofNullable(version);
	}

	public Set<Year> getYears() {
		return Set.copyOf(years);
	}

	public Optional<String> getTitle() {
		return Optional.ofNullable(title);
	}

	public Optional<String> getDescription() {
		return Optional.ofNullable(description);
	}

	public Optional<Destiny> getDestiny() {
		return Optional.ofNullable(destiny);
	}

	public Optional<String> getFccId() {
		return Optional.ofNullable(fccId);
	}

	public Optional<String> getHealth() {
		return Optional.ofNullable(health);
	}

	public Optional<Money> getPrice() {
		return Optional.ofNullable(price);
	}

	public Set<RetroId> getLot() {
		return Set.copyOf(lot);
	}

	public Optional<Money> getLotPrice() {
		return Optional.ofNullable(lotPrice);
	}

	public Optional<String> getSource() {
		return Optional.ofNullable(source);
	}

	public Optional<Tested> getTested() {
		return Optional.ofNullable(tested);
	}

	public Optional<Path> getAngledImage() {
		return Optional.ofNullable(angledImage);
	}

	public Optional<Path> getFrontImage() {
		return Optional.ofNullable(frontImage);
	}

	public Optional<Path> getBackImage() {
		return Optional.ofNullable(backImage);
	}

	public Optional<ISBN> getIsbn() {
		return Optional.ofNullable(isbn);
	}

	public Set<LanguageCode> getLanguages() {
		return Set.copyOf(languages);
	}

	public Set<RegionCode> getRegions() {
		return Set.copyOf(regions);
	}

	public Optional<MacAddress> getMacAddress() {
		return Optional.ofNullable(macAddress);
	}

	public Set<NintendoGameBoyCartridgeCode> getNintendoGameBoyCartridgeCodes() {
		return Set.copyOf(nintendoGameBoyCartridgeCodes);
	}

	public Set<PlayStationPortableDiscId> getPlayStationPortableDiscIds() {
		return Set.copyOf(playStationPortableDiscIds);
	}

	public Set<SegaGameGearCartridgeCode> getSegaGameGearCartridgeCodes() {
		return Set.copyOf(segaGameGearCartridgeCodes);
	}

	public Optional<String> getSerialNumber() {
		return Optional.ofNullable(serialNumber);
	}

	public Set<FloppyImageId> getFloppyImageIds() {
		return Set.copyOf(floppyImageIds);
	}

	public Set<Path> getFloppyImages() {
		return Set.copyOf(floppyImages);
	}

	public Optional<RamSet> getRamSet() {
		return Optional.ofNullable(ramSet);
	}

	public Set<DocumentId> getDocumentIds() {
		return Set.copyOf(documentIds);
	}

	public Optional<TheRetroWebId> getTheRetroWebId() {
		return Optional.ofNullable(theRetroWebId);
	}

	public Set<VideoConnector> getVideoConnectors() {
		return Set.copyOf(videoConnectors);
	}

	public Map<String, RetroAttribute> getAttributes() {
		return Map.copyOf(attributes);
	}
}
