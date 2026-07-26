package com.retrocrawler.mycollection.matchers;

import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.GearContext;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.mycollection.AttributeNames;
import com.retrocrawler.mycollection.model.ExpansionBus;

public final class GraphicsCardMatcher implements GearMatcher {

	@Override
	public Confidence matches(final GearContext context) {
		return context.getFact(AttributeNames.BUS, ExpansionBus.class).filter(ExpansionBus.AGP::equals)
				.map(bus -> Confidence.STRONG).orElse(Confidence.NONE);
	}
}
