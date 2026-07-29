package com.retrocrawler.mycollection.matchers;

import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.GearContext;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.model.hardware.ComputerFormFactor;
import com.retrocrawler.model.hardware.ExpansionBus;
import com.retrocrawler.mycollection.AttributeNames;

public final class MotherboardMatcher implements GearMatcher {

	@Override
	public Confidence matches(final GearContext context) {
		final boolean hasExpansionBus = context.getFacts(AttributeNames.BUS, ExpansionBus.class).isPresent();
		final boolean hasComputerForm = context
				.getFacts(AttributeNames.COMPUTER_FORM_FACTOR, ComputerFormFactor.class).isPresent();
		return hasExpansionBus && hasComputerForm ? Confidence.STRONG : Confidence.NONE;
	}
}
