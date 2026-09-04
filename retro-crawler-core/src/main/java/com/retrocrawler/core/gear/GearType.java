package com.retrocrawler.core.gear;

import java.util.Objects;
import java.util.regex.Pattern;

import com.retrocrawler.core.annotation.RetroAnyGear;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.util.ModelNames;
import com.retrocrawler.core.util.TypeName;

/** Stable model key and human name of one Gear type. */
public record GearType(String key, String name) {

	private static final Pattern KEY = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

	public GearType {
		key = Objects.requireNonNull(key, "key").strip();
		name = Objects.requireNonNull(name, "name").strip();
		if (!KEY.matcher(key).matches()) {
			throw new IllegalArgumentException(
					"Gear type key must contain lower-case words separated by hyphens: " + key);
		}
		if (name.isEmpty()) {
			throw new IllegalArgumentException("Gear type name must not be blank.");
		}
	}

	/**
	 * Derives type metadata from a Java Gear declaration and its annotation.
	 */
	public static GearType from(final Class<?> implementationType) {
		final Class<?> required = Objects.requireNonNull(implementationType, "implementationType");
		final RetroGear retroGear = required.getAnnotation(RetroGear.class);
		final RetroAnyGear anyGear = required.getAnnotation(RetroAnyGear.class);
		if (retroGear != null && anyGear != null) {
			throw new IllegalArgumentException(
					TypeName.simple(RetroGear.class) + " and " + TypeName.simple(RetroAnyGear.class)
							+ " must not both annotate Gear type " + TypeName.full(required) + ".");
		}
		final String declaredKey = retroGear != null ? retroGear.key() : anyGear == null ? "" : anyGear.key();
		final String declaredName = retroGear != null ? retroGear.name() : anyGear == null ? "" : anyGear.name();
		final String key = declaredKey.isBlank() ? ModelNames.typeKey(required) : declaredKey;
		final String name = declaredName.isBlank() ? ModelNames.displayName(required) : declaredName;
		return new GearType(key, name);
	}
}
