package com.retrocrawler.mycollection.gear.cpu;

import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.mycollection.matchers.ProcessorFamilyMatchers.Amd80486Matcher;

@RetroGear(value = Amd80486Matcher.class, key = "amd-80486", name = "AMD 80486")
public final class Amd80486 extends AmdProcessor {

	public Amd80486() {
	}
}
