package com.retrocrawler.mycollection.gear.cpu;

import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.mycollection.matchers.ProcessorFamilyMatchers.IntelPentiumMmxMatcher;

@RetroGear(value = IntelPentiumMmxMatcher.class, key = "intel-pentium-mmx", name = "Intel Pentium MMX")
public final class IntelPentiumMmx extends IntelPentium {

	public IntelPentiumMmx() {
	}
}
