package com.retrocrawler.mycollection.gear.cpu;

import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.mycollection.matchers.ProcessorFamilyMatchers.Intel80486Matcher;

@RetroGear(value = Intel80486Matcher.class, key = "intel-80486", name = "Intel 80486")
public final class Intel80486 extends IntelProcessor {

	public Intel80486() {
	}
}
