package com.retrocrawler.mycollection.facts;

import com.retrocrawler.core.gear.parser.EnumParser;
import com.retrocrawler.mycollection.catalog.Tested;

public final class TestedParser extends EnumParser<Tested> {

	public TestedParser() {
		super(Tested.class, (tested, raw) -> tested.value().equalsIgnoreCase(raw));
	}
}
