package com.retrocrawler.mycollection.matchers;

import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.GearContext;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.model.hardware.ComputerFormFactor;
import com.retrocrawler.model.hardware.ExpansionBus;
import com.retrocrawler.model.hardware.VideoConnector;
import com.retrocrawler.mycollection.AttributeNames;

public final class GraphicsCardMatcher implements GearMatcher {

	@Override
	public Confidence matches(final GearContext context) {
		final boolean hasOneExpansionBus = context.facts(AttributeNames.BUS, ExpansionBus.class)
				.filter(buses -> buses.size() == 1).isPresent();
		final boolean hasVideoConnector = context.facts(AttributeNames.VIDEO_CONNECTOR, VideoConnector.class)
				.isPresent();
		final boolean hasComputerForm = context.facts(AttributeNames.COMPUTER_FORM_FACTOR, ComputerFormFactor.class)
				.isPresent();
		return hasOneExpansionBus && hasVideoConnector && !hasComputerForm ? Confidence.STRONG : Confidence.NONE;
	}
}
