package com.retrocrawler.core.util;

import java.lang.reflect.Constructor;

public class Reflection {

	private Reflection() {
		// static utility class
	}

	public static <T> T newInstance(final Class<T> clazz) {
		try {
			final Constructor<T> ctor = clazz.getDeclaredConstructor();
			if (!ctor.canAccess(null)) {
				throw new IllegalArgumentException(clazz.getName() + " must have a public no-arg constructor.");
			}
			return ctor.newInstance();
		} catch (final Exception e) {
			throw new IllegalArgumentException("Cannot instantiate: " + clazz.getName(), e);
		}
	}

	public static Class<?> box(final Class<?> primitive) {
		if (primitive == boolean.class) {
			return Boolean.class;
		}
		if (primitive == byte.class) {
			return Byte.class;
		}
		if (primitive == short.class) {
			return Short.class;
		}
		if (primitive == int.class) {
			return Integer.class;
		}
		if (primitive == long.class) {
			return Long.class;
		}
		if (primitive == float.class) {
			return Float.class;
		}
		if (primitive == double.class) {
			return Double.class;
		}
		if (primitive == char.class) {
			return Character.class;
		}
		return primitive;
	}

}
