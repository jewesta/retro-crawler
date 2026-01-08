package com.retrocrawler.core.gear.injector;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.retrocrawler.core.gear.GearDescriptor;
import com.retrocrawler.core.gear.RetroAttributes;
import com.retrocrawler.core.util.RetroAttribute;

final class GearInjectionSession {

	private final GearDescriptor descriptor;
	private final Object gear;
	private final RetroAttributes attributes;

	private final Map<String, RetroAttribute> unassigned;

	GearInjectionSession(final GearDescriptor descriptor, final Object gear, final RetroAttributes attributes) {
		this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
		this.gear = Objects.requireNonNull(gear, "gear");
		this.attributes = Objects.requireNonNull(attributes, "attributes");

		/*
		 * We start with all attributes and remove successfully assigned (injected)
		 * facts along the way. What remains in the end are all clues (because only
		 * facts will ever get injected) and facts which the current gear didn't request
		 * (meaning: No related annotation).
		 * 
		 * In the end the AnyAttributeMode will decide what is actually used. We collect
		 * this information in any case, if not for statistical reasons.
		 */
		this.unassigned = attributes.getAll();
	}

	GearDescriptor getDescriptor() {
		return descriptor;
	}

	Class<?> getGearType() {
		return descriptor.getType();
	}

	Object getGear() {
		return gear;
	}

	RetroAttributes getAttributes() {
		return attributes;
	}

	/**
	 * @param attribute
	 * @return the attribute that got marked. If <code>null</code> is returned then
	 *         no attribute was marked unassigned. That could mean it was already
	 *         marked or it doesn't exist.
	 */
	RetroAttribute markAssigned(final RetroAttribute attribute) {
		return unassigned.remove(attribute.getKey());
	}

	Map<String, RetroAttribute> getUnassigned() {
		// Return copy to protect the original map
		return new LinkedHashMap<>(unassigned);
	}

}