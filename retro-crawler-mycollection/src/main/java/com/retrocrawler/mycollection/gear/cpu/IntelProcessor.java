package com.retrocrawler.mycollection.gear.cpu;

import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.mycollection.matchers.IntelProcessorMatcher;

@RetroGear(value = IntelProcessorMatcher.class, name = "Intel processor")
public class IntelProcessor extends Processor {

	public IntelProcessor() {
	}
}
