package com.retrocrawler.mycollection.gear.cpu;

import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.mycollection.matchers.ProcessorFamilyMatchers.Intel80286Matcher;

@RetroGear(value = Intel80286Matcher.class, key = "intel-80286", name = "Intel 80286")
public final class Intel80286 extends IntelProcessor {

	public Intel80286() {
	}
}
