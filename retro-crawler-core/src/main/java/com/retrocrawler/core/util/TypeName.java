package com.retrocrawler.core.util;

public class TypeName {

	private TypeName() {
		// static utility class
	}

	public static String name(final Class<?> type, final boolean simpleName) {
		if (type == null) {
			throw new NullPointerException("type must not be null");
		}
		final String name = simpleName ? type.getSimpleName() : type.getName();
		if (type.isAnnotation()) {
			return "@" + name;
		}
		return name;
	}

	public static String full(final Class<?> type) {
		return name(type, false);
	}

	public static String simple(final Class<?> type) {
		return name(type, true);
	}

}
