package com.retrocrawler.mycollection.gear.cpu;

import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.mycollection.matchers.ProcessorFamilyMatchers.IntelPentiumMatcher;

@RetroGear(value = IntelPentiumMatcher.class, key = "intel-pentium", name = "Intel Pentium")
public class IntelPentium extends IntelProcessor {

	public IntelPentium() {
	}
}
