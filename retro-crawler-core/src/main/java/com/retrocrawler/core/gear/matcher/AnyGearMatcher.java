package com.retrocrawler.core.gear.matcher;

import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.GearContext;

public final class AnyGearMatcher implements GearMatcher {

	@Override
	public Confidence matches(final GearContext context) {
		// Accept any artifact, but other types take precedence (fallback-type)
		return Confidence.WEAK;
	}

}
