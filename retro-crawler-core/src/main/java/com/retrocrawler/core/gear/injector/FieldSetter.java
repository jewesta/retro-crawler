package com.retrocrawler.core.gear.injector;

import java.lang.reflect.Field;
import java.util.Map;

import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.util.RetroAttribute;
import com.retrocrawler.core.util.TypeName;

final class FieldSetter {

	private FieldSetter() {
		// static utility class
	}

	public static void setField(final Object target, final Field field, final Object value) {
		try {
			if (!field.canAccess(target)) {
				field.setAccessible(true);
			}
			field.set(target, value);
		} catch (final Exception e) {
			throw new IllegalStateException(
					"Cannot set field " + field.getName() + " on " + TypeName.full(target.getClass()), e);
		}
	}

	public static void setAnyAttributeMap(final Object gear, final Field anyField,
			final Map<String, RetroAttribute> payload) {

		try {
			if (!anyField.canAccess(gear)) {
				anyField.setAccessible(true);
			}
			anyField.set(gear, payload);
		} catch (final Exception e) {
			throw new IllegalStateException("Cannot set " + TypeName.simple(RetroAnyAttribute.class) + " field "
					+ anyField.getName() + " on " + TypeName.full(gear.getClass()), e);
		}
	}

}