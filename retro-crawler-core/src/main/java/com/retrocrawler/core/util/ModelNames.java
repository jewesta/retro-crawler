package com.retrocrawler.core.util;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Derives stable keys and human names from Java model declarations. */
public final class ModelNames {

	private static final Pattern ACRONYM_BOUNDARY = Pattern.compile("([A-Z]+)([A-Z][a-z])");

	private static final Pattern WORD_BOUNDARY = Pattern.compile("([a-z0-9])([A-Z])");

	private static final Pattern SEPARATORS = Pattern.compile("[-_\\s]+");

	private ModelNames() {
		// static utility class
	}

	/** Derives a lower-case, hyphen-separated key from a Java type name. */
	public static String typeKey(final Class<?> type) {
		return words(simpleName(type)).replace(' ', '-');
	}

	/** Derives a lower-case, space-separated display name from a Java type. */
	public static String displayName(final Class<?> type) {
		return words(simpleName(type));
	}

	/** Derives a lower-case, space-separated display name from a Java field. */
	public static String displayName(final Field field) {
		return displayName(Objects.requireNonNull(field, "field").getName());
	}

	/** Derives a lower-case, space-separated display name from a Java name. */
	public static String displayName(final String javaName) {
		return words(javaName);
	}

	private static String simpleName(final Class<?> type) {
		final String simpleName = Objects.requireNonNull(type, "type").getSimpleName();
		if (simpleName.isBlank()) {
			throw new IllegalArgumentException("A model type must have a simple Java name: " + type.getName());
		}
		return simpleName;
	}

	private static String words(final String javaName) {
		final String required = Objects.requireNonNull(javaName, "javaName").strip();
		if (required.isEmpty()) {
			throw new IllegalArgumentException("A Java model name must not be blank.");
		}
		final String separatorsNormalized = SEPARATORS.matcher(required).replaceAll(" ");
		final String acronymsSeparated = ACRONYM_BOUNDARY.matcher(separatorsNormalized).replaceAll("$1 $2");
		return WORD_BOUNDARY.matcher(acronymsSeparated).replaceAll("$1 $2").toLowerCase(Locale.ROOT);
	}
}
