package com.retrocrawler.mycollection.matchers;

import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.GearContext;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.model.hardware.ComputerFormFactor;
import com.retrocrawler.model.measurement.Power;
import com.retrocrawler.mycollection.AttributeNames;

public final class PowerSupplyMatcher implements GearMatcher {

	@Override
	public Confidence matches(final GearContext context) {
		final boolean hasPower = context.fact(AttributeNames.POWER, Power.class).isPresent();
		final boolean hasComputerForm = context.facts(AttributeNames.COMPUTER_FORM_FACTOR, ComputerFormFactor.class)
				.isPresent();
		return hasPower && hasComputerForm ? Confidence.STRONG : Confidence.NONE;
	}
}
