package com.retrocrawler.mycollection.gear;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.gear.parser.StringParser;
import com.retrocrawler.core.util.RetroAttribute;
import com.retrocrawler.mycollection.AttributeNames;
import com.retrocrawler.mycollection.facts.FloppyImageIdParser;
import com.retrocrawler.mycollection.facts.RetroIdParser;
import com.retrocrawler.mycollection.model.ExpansionBus;
import com.retrocrawler.mycollection.model.FloppyImageId;
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

	@RetroFact(key = AttributeNames.DESCRIPTION, optional = true)
	private String description;

	@RetroFact(key = AttributeNames.IMAGE_ANGLED, optional = true)
	private String angledImage;

	@RetroFact(key = AttributeNames.IMAGE_FRONT, optional = true)
	private String frontImage;

	@RetroFact(key = AttributeNames.IMAGE_BACK, optional = true)
	private String backImage;

	@RetroFact(key = AttributeNames.FLOPPY_IMAGE_ID, parser = FloppyImageIdParser.class, optional = true)
	private Set<FloppyImageId> floppyImageIds = Set.of();

	@RetroFact(key = AttributeNames.FLOPPY_IMAGES, parser = StringParser.class, optional = true)
	private Set<String> floppyImages = Set.of();

	@RetroFact(key = AttributeNames.WEB_REFERENCES, parser = StringParser.class, optional = true)
	private Set<String> webReferences = Set.of();

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

	public Set<FloppyImageId> getFloppyImageIds() {
		return Set.copyOf(floppyImageIds);
	}

	public Set<String> getFloppyImages() {
		return Set.copyOf(floppyImages);
	}

	public Set<String> getWebReferences() {
		return Set.copyOf(webReferences);
	}

	public Map<String, RetroAttribute> getAttributes() {
		return Map.copyOf(attributes);
	}
}
