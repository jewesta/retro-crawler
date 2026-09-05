package com.retrocrawler.mycollection.gear.cpu;

import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.mycollection.matchers.ProcessorFamilyMatchers.AmdK6IIIMatcher;

@RetroGear(value = AmdK6IIIMatcher.class, key = "amd-k6-iii", name = "AMD K6-III")
public final class AmdK6III extends AmdK6 {

	public AmdK6III() {
	}
}
