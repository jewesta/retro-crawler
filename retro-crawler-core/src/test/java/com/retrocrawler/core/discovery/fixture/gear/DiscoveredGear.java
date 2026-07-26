package com.retrocrawler.core.discovery.fixture.gear;

import java.util.HashMap;
import java.util.Map;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.discovery.fixture.InitializationProbe;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.util.RetroAttribute;

@RetroGear(AnyGearMatcher.class)
public class DiscoveredGear {

	static {
		InitializationProbe.markGearInitialized();
	}

	@RetroAnyAttribute
	private final Map<String, RetroAttribute> attributes = new HashMap<>();

	public DiscoveredGear() {
	}
}
