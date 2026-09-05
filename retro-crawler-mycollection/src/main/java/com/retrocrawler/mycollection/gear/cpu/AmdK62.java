package com.retrocrawler.mycollection.gear.cpu;

import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.mycollection.matchers.ProcessorFamilyMatchers.AmdK62Matcher;

@RetroGear(value = AmdK62Matcher.class, key = "amd-k6-2", name = "AMD K6-2")
public final class AmdK62 extends AmdK6 {

	public AmdK62() {
	}
}
