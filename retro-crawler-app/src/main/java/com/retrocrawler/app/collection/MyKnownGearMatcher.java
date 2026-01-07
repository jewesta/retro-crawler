package com.retrocrawler.app.collection;

import com.retrocrawler.app.collection.model.ID;
import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.GearContext;
import com.retrocrawler.core.gear.matcher.GearMatcher;

public class MyKnownGearMatcher implements GearMatcher {

	@Override
	public Confidence matches(final GearContext context) {
		if (context.getFact("id", ID.class).isPresent()) {
			return Confidence.EXACT;
		}
		return Confidence.NONE;
	}

}
