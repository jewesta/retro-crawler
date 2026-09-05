package com.retrocrawler.mycollection.gear.cpu;

import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.mycollection.matchers.ProcessorFamilyMatchers.AmdK6Matcher;

@RetroGear(value = AmdK6Matcher.class, key = "amd-k6", name = "AMD K6")
public class AmdK6 extends AmdProcessor {

	public AmdK6() {
	}
}
