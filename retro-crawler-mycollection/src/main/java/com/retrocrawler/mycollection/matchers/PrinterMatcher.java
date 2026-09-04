package com.retrocrawler.mycollection.matchers;

import com.retrocrawler.core.gear.Confidence;
import com.retrocrawler.core.gear.GearContext;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.model.hardware.PrinterType;
import com.retrocrawler.mycollection.AttributeNames;

public final class PrinterMatcher implements GearMatcher {

	@Override
	public Confidence matches(final GearContext context) {
		return context.fact(AttributeNames.PRINTER_TYPE, PrinterType.class).isPresent() ? Confidence.STRONG
				: Confidence.NONE;
	}
}
