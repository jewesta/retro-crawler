package com.retrocrawler.mycollection.gear;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.gear.parser.StringParser;
import com.retrocrawler.core.util.RetroAttribute;
import com.retrocrawler.model.hardware.ExpansionBus;
import com.retrocrawler.model.hardware.ExpansionBusParser;
import com.retrocrawler.model.identifier.ISBN;
import com.retrocrawler.model.identifier.ISBNParser;
import com.retrocrawler.model.identifier.MacAddress;
import com.retrocrawler.model.identifier.MacAddressParser;
import com.retrocrawler.model.identifier.TheRetroWebId;
import com.retrocrawler.model.identifier.TheRetroWebIdParser;
import com.retrocrawler.model.measurement.DataCapacity;
import com.retrocrawler.model.measurement.DataCapacityParser;
import com.retrocrawler.mycollection.AttributeNames;
import com.retrocrawler.mycollection.catalog.FloppyImageId;
import com.retrocrawler.mycollection.catalog.RetroId;
import com.retrocrawler.mycollection.facts.FloppyImageIdParser;
import com.retrocrawler.mycollection.facts.RamSetParser;
import com.retrocrawler.mycollection.facts.RetroIdParser;
import com.retrocrawler.mycollection.memory.RamSet;

public abstract class MyGear {

	@RetroFact(key = "@folder", optional = false)
	private String folderName;

	@com.retrocrawler.core.annotation.RetroId
	@RetroFact(key = AttributeNames.RETRO_ID, parser = RetroIdParser.class, strict = false, optional = true)
	private RetroId retroId;

	@RetroFact(key = AttributeNames.BUS, parser = ExpansionBusParser.class, strict = false, optional = true)
	private ExpansionBus bus;

	@RetroFact(key = AttributeNames.CAPACITY, parser = DataCapacityParser.class, strict = false, optional = true)
	private DataCapacity capacity;

	@RetroFact(key = AttributeNames.TITLE, optional = true)
	private String title;

	@RetroFact(key = AttributeNames.DESCRIPTION, optional = true)
	private String description;

	@RetroFact(key = AttributeNames.IMAGE_ANGLED, optional = true)
	private String angledImage;

	@RetroFact(key = AttributeNames.IMAGE_FRONT, optional = true)
	private String frontImage;

	@RetroFact(key = AttributeNames.IMAGE_BACK, optional = true)
	private String backImage;

	@RetroFact(key = AttributeNames.ISBN, parser = ISBNParser.class, optional = true)
	private ISBN isbn;

	@RetroFact(key = AttributeNames.MAC_ADDRESS, parser = MacAddressParser.class, optional = true)
	private MacAddress macAddress;

	@RetroFact(key = AttributeNames.SERIAL_NUMBER, parser = StringParser.class, optional = true)
	private String serialNumber;

	@RetroFact(key = AttributeNames.FLOPPY_IMAGE_ID, parser = FloppyImageIdParser.class, optional = true)
	private Set<FloppyImageId> floppyImageIds = Set.of();

	@RetroFact(key = AttributeNames.FLOPPY_IMAGES, parser = StringParser.class, optional = true)
	private Set<String> floppyImages = Set.of();

	@RetroFact(key = AttributeNames.RAM_SET, parser = RamSetParser.class, optional = true)
	private RamSet ramSet;

	@RetroFact(key = AttributeNames.THE_RETRO_WEB_ID, parser = TheRetroWebIdParser.class, optional = true)
	private TheRetroWebId theRetroWebId;

	@RetroAnyAttribute
	private final Map<String, RetroAttribute> attributes = new HashMap<>();

	public String getFolderName() {
		return folderName;
	}

	public Optional<RetroId> getRetroId() {
		return Optional.ofNullable(retroId);
	}

	public Optional<ExpansionBus> getBus() {
		return Optional.ofNullable(bus);
	}

	public Optional<DataCapacity> getCapacity() {
		return Optional.ofNullable(capacity);
	}

	public Optional<String> getTitle() {
		return Optional.ofNullable(title);
	}

	public Optional<String> getDescription() {
		return Optional.ofNullable(description);
	}

	public Optional<String> getAngledImage() {
		return Optional.ofNullable(angledImage);
	}

	public Optional<String> getFrontImage() {
		return Optional.ofNullable(frontImage);
	}

	public Optional<String> getBackImage() {
		return Optional.ofNullable(backImage);
	}

	public Optional<ISBN> getIsbn() {
		return Optional.ofNullable(isbn);
	}

	public Optional<MacAddress> getMacAddress() {
		return Optional.ofNullable(macAddress);
	}

	public Optional<String> getSerialNumber() {
		return Optional.ofNullable(serialNumber);
	}

	public Set<FloppyImageId> getFloppyImageIds() {
		return Set.copyOf(floppyImageIds);
	}

	public Set<String> getFloppyImages() {
		return Set.copyOf(floppyImages);
	}

	public Optional<RamSet> getRamSet() {
		return Optional.ofNullable(ramSet);
	}

	public Optional<TheRetroWebId> getTheRetroWebId() {
		return Optional.ofNullable(theRetroWebId);
	}

	public Map<String, RetroAttribute> getAttributes() {
		return Map.copyOf(attributes);
	}
}
