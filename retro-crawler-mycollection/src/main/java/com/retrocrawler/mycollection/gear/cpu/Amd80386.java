package com.retrocrawler.mycollection.gear.cpu;

import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.mycollection.matchers.ProcessorFamilyMatchers.Amd80386Matcher;

@RetroGear(value = Amd80386Matcher.class, key = "amd-80386", name = "AMD 80386")
public final class Amd80386 extends AmdProcessor {

	public Amd80386() {
	}
}
