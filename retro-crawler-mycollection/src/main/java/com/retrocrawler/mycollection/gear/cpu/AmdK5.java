package com.retrocrawler.mycollection.gear.cpu;

import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.mycollection.matchers.ProcessorFamilyMatchers.AmdK5Matcher;

@RetroGear(value = AmdK5Matcher.class, key = "amd-k5", name = "AMD K5")
public final class AmdK5 extends AmdProcessor {

	public AmdK5() {
	}
}
