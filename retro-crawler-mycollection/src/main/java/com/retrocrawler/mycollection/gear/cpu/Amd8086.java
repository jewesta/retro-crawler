package com.retrocrawler.mycollection.gear.cpu;

import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.mycollection.matchers.ProcessorFamilyMatchers.Amd8086Matcher;

@RetroGear(value = Amd8086Matcher.class, key = "amd-8086", name = "AMD 8086")
public final class Amd8086 extends AmdProcessor {

	public Amd8086() {
	}
}
