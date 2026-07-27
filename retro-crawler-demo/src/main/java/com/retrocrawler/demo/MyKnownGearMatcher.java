package com.retrocrawler.demo;

import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.GearContext;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.demo.catalog.DemoId;

public class MyKnownGearMatcher implements GearMatcher {

	@Override
	public Confidence matches(final GearContext context) {
		if (context.getFact("id", DemoId.class).isPresent()) {
			return Confidence.EXACT;
		}
		return Confidence.NONE;
	}

}
