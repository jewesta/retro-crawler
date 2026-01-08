package com.retrocrawler.core.gear.injector;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.gear.Fact;
import com.retrocrawler.core.gear.GearDescriptor;
import com.retrocrawler.core.gear.RetroAttributes;
import com.retrocrawler.core.util.RetroAttribute;

final class UnassignedPolicy {

	GearInjectionSession createSession(final GearDescriptor descriptor, final Object gear,
			final RetroAttributes attributes) {
		Objects.requireNonNull(descriptor, "descriptor");
		Objects.requireNonNull(gear, "gear");
		Objects.requireNonNull(attributes, "attributes");

		final Map<String, Clue> cluesByKey = new LinkedHashMap<>();
		for (final Clue clue : attributes.getClues()) {
			cluesByKey.put(clue.getKey(), clue);
		}

		final Map<String, Fact> factsByKey = new LinkedHashMap<>();
		for (final Fact fact : attributes.getFacts()) {
			factsByKey.put(fact.getKey(), fact);
		}

		final Map<String, RetroAttribute> unassigned = new LinkedHashMap<>();
		for (final Clue clue : attributes.getClues()) {
			unassigned.put(clue.getKey(), clue);
		}
		for (final Fact fact : attributes.getFacts()) {
			unassigned.put(fact.getKey(), fact);
		}

		return new GearInjectionSession(descriptor, gear, attributes, factsByKey, cluesByKey, unassigned);
	}

}