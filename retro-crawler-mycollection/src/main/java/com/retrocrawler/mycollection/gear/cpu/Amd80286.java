package com.retrocrawler.mycollection.gear.cpu;

import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.mycollection.matchers.ProcessorFamilyMatchers.Amd80286Matcher;

@RetroGear(value = Amd80286Matcher.class, key = "amd-80286", name = "AMD 80286")
public final class Amd80286 extends AmdProcessor {

	public Amd80286() {
	}
}
