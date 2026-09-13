package com.retrocrawler.mycollection.gear.cpu;

import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.mycollection.matchers.ProcessorFamilyMatchers.Intel80386Matcher;

@RetroGear(value = Intel80386Matcher.class, key = "intel-80386", name = "Intel 80386")
public final class Intel80386 extends IntelProcessor {

	public Intel80386() {
	}
}
