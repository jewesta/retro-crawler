package com.retrocrawler.core.gear.injector;

import java.util.Map;
import java.util.Objects;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.gear.Fact;
import com.retrocrawler.core.gear.GearDescriptor;
import com.retrocrawler.core.gear.RetroAttributes;
import com.retrocrawler.core.util.RetroAttribute;

final class GearInjectionSession {

	private final GearDescriptor descriptor;
	private final Object gear;
	private final RetroAttributes attributes;

	private final Map<String, Fact> allFactsByKey;
	private final Map<String, Clue> allCluesByKey;

	private final Map<String, RetroAttribute> unassigned;

	GearInjectionSession(final GearDescriptor descriptor, final Object gear, final RetroAttributes attributes,
			final Map<String, Fact> allFactsByKey, final Map<String, Clue> allCluesByKey,
			final Map<String, RetroAttribute> unassigned) {

		this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
		this.gear = Objects.requireNonNull(gear, "gear");
		this.attributes = Objects.requireNonNull(attributes, "attributes");
		this.allFactsByKey = Objects.requireNonNull(allFactsByKey, "allFactsByKey");
		this.allCluesByKey = Objects.requireNonNull(allCluesByKey, "allCluesByKey");
		this.unassigned = Objects.requireNonNull(unassigned, "unassigned");
	}

	GearDescriptor descriptor() {
		return descriptor;
	}

	Class<?> gearType() {
		return descriptor.getType();
	}

	Object gear() {
		return gear;
	}

	RetroAttributes attributes() {
		return attributes;
	}

	Map<String, Fact> allFactsByKey() {
		return allFactsByKey;
	}

	Map<String, Clue> allCluesByKey() {
		return allCluesByKey;
	}

	Map<String, RetroAttribute> unassigned() {
		return unassigned;
	}

}