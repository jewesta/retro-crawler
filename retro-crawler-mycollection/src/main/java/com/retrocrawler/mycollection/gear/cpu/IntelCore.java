package com.retrocrawler.mycollection.gear.cpu;

import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.mycollection.matchers.ProcessorFamilyMatchers.IntelCoreMatcher;

@RetroGear(value = IntelCoreMatcher.class, key = "intel-core", name = "Intel Core")
public final class IntelCore extends IntelProcessor {

	public IntelCore() {
	}
}
