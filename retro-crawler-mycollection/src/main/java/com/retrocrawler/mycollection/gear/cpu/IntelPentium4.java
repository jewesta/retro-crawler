package com.retrocrawler.mycollection.gear.cpu;

import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.mycollection.matchers.ProcessorFamilyMatchers.IntelPentium4Matcher;

@RetroGear(value = IntelPentium4Matcher.class, key = "intel-pentium-4", name = "Intel Pentium 4")
public final class IntelPentium4 extends IntelPentium {

	public IntelPentium4() {
	}
}
