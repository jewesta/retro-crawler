package com.retrocrawler.core.gear;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.util.Descriptor;

public abstract class AttributeDescriptor implements Descriptor {

	protected String key;

	private final boolean optional;

	private final Field field;

	protected AttributeDescriptor(final Field field, final boolean optional) {
		this.field = Objects.requireNonNull(field, "field");
		this.optional = optional;
	}

	public String getKey() {
		return key;
	}

	public boolean isOptional() {
		return optional;
	}

	public Field getField() {
		return field;
	}

	public Optional<Class<?>> getSingleGenericArgument() {
		final Type t = field.getGenericType();
		if (!(t instanceof ParameterizedType)) {
			return Optional.empty();
		}

		final ParameterizedType pt = (ParameterizedType) t;
		final Type[] args = pt.getActualTypeArguments();
		if (args == null || args.length != 1) {
			return Optional.empty();
		}

		final Type arg = args[0];
		if (arg instanceof Class<?>) {
			return Optional.of((Class<?>) arg);
		}

		if (arg instanceof ParameterizedType) {
			final Type raw = ((ParameterizedType) arg).getRawType();
			if (raw instanceof Class<?>) {
				return Optional.of((Class<?>) raw);
			}
		}

		return Optional.empty();
	}

	protected static String effectiveKey(final Field field, final String annotationKey) {
		if (annotationKey == null || annotationKey.isBlank()) {
			return field.getName();
		}
		return annotationKey.trim();
	}

}