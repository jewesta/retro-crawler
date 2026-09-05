package com.retrocrawler.mycollection.gear.cpu;

import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.mycollection.matchers.AmdProcessorMatcher;

@RetroGear(value = AmdProcessorMatcher.class, name = "AMD processor")
public class AmdProcessor extends Processor {

	public AmdProcessor() {
	}
}
