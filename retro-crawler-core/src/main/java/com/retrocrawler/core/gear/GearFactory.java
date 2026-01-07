package com.retrocrawler.core.gear;

public interface GearFactory {

	/**
	 * Returns a fully configured piece of your retro collection.
	 */
	Object create(GearContext context);

}
