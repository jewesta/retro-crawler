package com.retrocrawler.core.discovery.fixture;

public final class InitializationProbe {

	private static boolean gearInitialized;

	private InitializationProbe() {
		// static test utility
	}

	public static void markGearInitialized() {
		gearInitialized = true;
	}

	public static boolean wasGearInitialized() {
		return gearInitialized;
	}

	public static void reset() {
		gearInitialized = false;
	}
}
