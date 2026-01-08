package com.retrocrawler.core.gear.injector;

import java.lang.reflect.Field;
import java.util.Map.Entry;
import java.util.Objects;

import com.retrocrawler.core.gear.FactDescriptor;
import com.retrocrawler.core.gear.GearDescriptor;
import com.retrocrawler.core.util.RetroAttribute;

final class DeclaredFactsInjector implements Injector {

	private final RetroAttributeAdapter adapter;

	DeclaredFactsInjector(final RetroAttributeAdapter adapter) {
		this.adapter = Objects.requireNonNull(adapter, "adapter");
	}

	@Override
	public void inject(final GearInjectionSession session) {

		final GearDescriptor definition = session.descriptor();

		for (final Entry<String, FactDescriptor> entry : definition.getAttributes().entrySet()) {
			final String key = entry.getKey();
			final FactDescriptor descriptor = entry.getValue();
			final Field field = descriptor.getField();

			final RetroAttribute attribute = session.attributes().get(key);
			session.unassigned().remove(key);

			if (attribute == null) {
				AttributeAsserter.assertMissingAllowed(session.gearType(), descriptor, key);
				continue;
			}

			final Object injected = adapter.adaptAttributeToField(session.gearType(), field, key, attribute);
			if (injected != null) {
				FieldSetter.setField(session.gear(), field, injected);
			}
		}
	}
}