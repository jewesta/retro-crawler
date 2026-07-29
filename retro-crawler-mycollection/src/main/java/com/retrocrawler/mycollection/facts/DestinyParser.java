package com.retrocrawler.mycollection.facts;

import com.retrocrawler.core.gear.parser.EnumParser;
import com.retrocrawler.mycollection.catalog.Destiny;

public final class DestinyParser extends EnumParser<Destiny> {

	public DestinyParser() {
		super(Destiny.class, (destiny, raw) -> destiny.value().equalsIgnoreCase(raw));
	}
}
