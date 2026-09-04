package com.retrocrawler.mycollection.matchers;

import com.retrocrawler.core.gear.Confidence;
import com.retrocrawler.core.gear.GearContext;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.model.hardware.AmdProcessorMarking;
import com.retrocrawler.mycollection.AttributeNames;

public final class AmdProcessorMatcher implements GearMatcher {

	@Override
	public Confidence matches(final GearContext context) {
		return context.fact(AttributeNames.PROCESSOR_MARKING, AmdProcessorMarking.class).isPresent() ? Confidence.EXACT
				: Confidence.NONE;
	}
}
