package com.retrocrawler.core.gear.injector;

import java.lang.reflect.Field;

import com.retrocrawler.core.gear.FactDescriptor;
import com.retrocrawler.core.util.TypeName;

final class AttributeAsserter {

	private AttributeAsserter() {
		// static utility class
	}

	public static void assertMissingAllowed(final Class<?> gearType, final FactDescriptor descriptor, final String key) {

		final Field field = descriptor.getField();

		if (!descriptor.isOptional()) {
			throw new IllegalStateException(
					"Missing required attribute for key '" + key + "' on gear " + TypeName.full(gearType) + " field "
							+ field.getName() + ". Either provide a matching attribute or mark the field as optional.");
		}

		if (field.getType().isPrimitive()) {
			throw new IllegalStateException("Missing attribute for primitive field " + field.getName() + " (key '" + key
					+ "') on gear " + TypeName.full(gearType)
					+ ". Primitive fields cannot be null; make it non-primitive or provide the attribute.");
		}
	}

}