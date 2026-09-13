package com.retrocrawler.mycollection.gear.cpu;

import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.mycollection.matchers.ProcessorFamilyMatchers.Amd5x86Matcher;

@RetroGear(value = Amd5x86Matcher.class, key = "amd-5x86", name = "AMD 5x86")
public final class Amd5x86 extends AmdProcessor {

	public Amd5x86() {
	}
}
