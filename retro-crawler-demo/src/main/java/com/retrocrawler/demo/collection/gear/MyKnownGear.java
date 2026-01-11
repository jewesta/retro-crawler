package com.retrocrawler.demo.collection.gear;

import java.nio.file.Path;
import java.util.Optional;

import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.demo.collection.AttributeNames;
import com.retrocrawler.demo.collection.MyKnownGearMatcher;
import com.retrocrawler.demo.collection.facts.IDParser;
import com.retrocrawler.demo.collection.model.ID;

@RetroGear(MyKnownGearMatcher.class)
public class MyKnownGear extends MyRetroGear {

	// @RetroId
	@RetroFact(parser = IDParser.class, strict = false, optional = false)
	public ID id;

	@RetroFact
	public String trw;

	@RetroFact
	public String title;

	@RetroFact(key = AttributeNames.PIC_FRONT, optional = true)
	public String picFront;

	public ID getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public Optional<Path> getPicFront() {
		return Optional.ofNullable(picFront).map(Path::of);
	}

}
