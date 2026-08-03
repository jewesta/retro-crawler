package com.retrocrawler.mycollection.matchers;

import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.GearContext;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.mycollection.AttributeNames;
import com.retrocrawler.mycollection.gear.GearKind;

public final class HardDiskDriveMatcher implements GearMatcher {

	@Override
	public Confidence matches(final GearContext context) {
		return context.fact(AttributeNames.GEAR_KIND, GearKind.class)
				.filter(GearKind.HARD_DISK_DRIVE::equals)
				.map(ignored -> Confidence.EXACT)
				.orElse(Confidence.NONE);
	}
}
