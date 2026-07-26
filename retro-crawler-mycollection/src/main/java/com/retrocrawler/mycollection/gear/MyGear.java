package com.retrocrawler.mycollection.gear;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.util.RetroAttribute;
import com.retrocrawler.mycollection.AttributeNames;
import com.retrocrawler.mycollection.facts.RetroIdParser;
import com.retrocrawler.mycollection.model.ExpansionBus;
import com.retrocrawler.mycollection.model.RetroId;

public abstract class MyGear {

	@RetroFact(key = "@folder", optional = false)
	private String folderName;

	@com.retrocrawler.core.annotation.RetroId
	@RetroFact(key = AttributeNames.RETRO_ID, parser = RetroIdParser.class, strict = false, optional = true)
	private RetroId retroId;

	@RetroFact(key = AttributeNames.BUS, strict = false, optional = true)
	private ExpansionBus bus;

	@RetroFact(key = AttributeNames.TITLE, optional = true)
	private String title;

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

	public Optional<String> getTitle() {
		return Optional.ofNullable(title);
	}

	public Map<String, RetroAttribute> getAttributes() {
		return Map.copyOf(attributes);
	}
}
