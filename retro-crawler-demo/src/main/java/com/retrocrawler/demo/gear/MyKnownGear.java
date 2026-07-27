package com.retrocrawler.demo.gear;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.demo.AttributeNames;
import com.retrocrawler.demo.MyKnownGearMatcher;
import com.retrocrawler.demo.catalog.DemoId;
import com.retrocrawler.demo.facts.DemoIdParser;
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

@RetroGear(MyKnownGearMatcher.class)
public class MyKnownGear extends MyRetroGear {

	@com.retrocrawler.core.annotation.RetroId
	@RetroFact(parser = DemoIdParser.class, strict = false, optional = false)
	public DemoId id;

	@RetroFact(key = AttributeNames.THE_RETRO_WEB_ID, parser = TheRetroWebIdParser.class)
	public TheRetroWebId trw;

	@RetroFact(key = AttributeNames.TITLE)
	public String title;

	@RetroFact(key = AttributeNames.BUS, parser = ExpansionBusParser.class, strict = false)
	private Set<ExpansionBus> expansionBuses = Set.of();

	@RetroFact(key = AttributeNames.CAPACITY, parser = DataCapacityParser.class, strict = false)
	private DataCapacity capacity;

	@RetroFact(key = AttributeNames.ISBN, parser = ISBNParser.class)
	private ISBN isbn;

	@RetroFact(key = AttributeNames.MAC_ADDRESS, parser = MacAddressParser.class)
	private MacAddress macAddress;

	@RetroFact(key = AttributeNames.PIC_FRONT, optional = true)
	public String picFront;

	public DemoId getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public Optional<Path> getPicFront() {
		return Optional.ofNullable(picFront).map(Path::of);
	}

	public Optional<TheRetroWebId> getTheRetroWebId() {
		return Optional.ofNullable(trw);
	}

	public Set<ExpansionBus> getExpansionBuses() {
		return Set.copyOf(expansionBuses);
	}

	public Optional<DataCapacity> getCapacity() {
		return Optional.ofNullable(capacity);
	}

	public Optional<ISBN> getIsbn() {
		return Optional.ofNullable(isbn);
	}

	public Optional<MacAddress> getMacAddress() {
		return Optional.ofNullable(macAddress);
	}

}
