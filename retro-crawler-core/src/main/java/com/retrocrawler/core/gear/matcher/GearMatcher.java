package com.retrocrawler.core.gear.matcher;

import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.GearContext;

public interface GearMatcher {

	Confidence matches(GearContext context);

}
