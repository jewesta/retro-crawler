package com.retrocrawler.core.gear.injector;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.annotation.RetroId;
import com.retrocrawler.core.archive.clues.InternalClueKeys;
import com.retrocrawler.core.gear.FactDescriptor;
import com.retrocrawler.core.gear.GearDescriptor;
import com.retrocrawler.core.util.RetroAttribute;
import com.retrocrawler.core.util.TypeName;

final class StandaloneIdInjector implements Injector {

	private final RetroAttributeAdapter adapter;

	StandaloneIdInjector(final RetroAttributeAdapter adapter) {
		this.adapter = Objects.requireNonNull(adapter, "adapter");
	}

	@Override
	public void inject(final GearInjectionSession session) {
		final GearDescriptor definition = session.descriptor();
		final Optional<Field> idFieldOpt = definition.idField();
		if (idFieldOpt.isEmpty()) {
			return;
		}
		final Field idField = idFieldOpt.get();
		if (isIdFieldAlreadyHandledByFact(definition, idField)) {
			return;
		}
		final RetroAttribute attribute = session.attributes().get(InternalClueKeys.ID);
		if (attribute == null) {
			throw new IllegalStateException("Missing technical id clue for key '" + InternalClueKeys.ID
					+ "' required by standalone " + TypeName.simple(RetroId.class) + " on gear "
					+ TypeName.full(session.gearType()) + " field " + idField.getName() + ".");
		}
		final Object inject = adapter.adaptAttributeToField(session.gearType(), idField, InternalClueKeys.ID,
				attribute);
		Objects.requireNonNull(inject);
		FieldSetter.setField(session.gear(), idField, inject);
		session.markAssigned(attribute);
	}

	private static boolean isIdFieldAlreadyHandledByFact(final GearDescriptor definition, final Field idField) {
		for (final FactDescriptor def : definition.attributes().values()) {
			if (def.field().equals(idField)) {
				return true;
			}
		}
		return false;
	}
}
