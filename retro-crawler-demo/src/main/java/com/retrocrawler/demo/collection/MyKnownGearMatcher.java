package com.retrocrawler.demo.collection;

import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.GearContext;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.demo.collection.model.ID;

public class MyKnownGearMatcher implements GearMatcher {

	@Override
	public Confidence matches(final GearContext context) {
		if (context.getFact("id", ID.class).isPresent()) {
			return Confidence.EXACT;
		}
		return Confidence.NONE;
	}

}
