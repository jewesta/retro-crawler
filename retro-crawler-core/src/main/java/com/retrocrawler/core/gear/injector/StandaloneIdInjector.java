package com.retrocrawler.core.gear.injector;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.annotation.RetroId;
import com.retrocrawler.core.archive.clues.Clue;
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
		final GearDescriptor definition = session.getDescriptor();
		final Optional<Field> idFieldOpt = definition.getIdField();
		if (idFieldOpt.isEmpty()) {
			return;
		}
		final Field idField = idFieldOpt.get();
		if (isIdFieldAlreadyHandledByFact(definition, idField)) {
			return;
		}
		final RetroAttribute attribute = session.getAttributes().get(Clue.KEY_INTERNAL_ID);
		if (attribute == null) {
			throw new IllegalStateException("Missing technical id clue for key '" + Clue.KEY_INTERNAL_ID
					+ "' required by standalone " + TypeName.simple(RetroId.class) + " on gear "
					+ TypeName.full(session.getGearType()) + " field " + idField.getName() + ".");
		}
		final Object inject = adapter.adaptAttributeToField(session.getGearType(), idField, Clue.KEY_INTERNAL_ID,
				attribute);
		Objects.requireNonNull(inject);
		FieldSetter.setField(session.getGear(), idField, inject);
		session.markAssigned(attribute);
	}

	private static boolean isIdFieldAlreadyHandledByFact(final GearDescriptor definition, final Field idField) {
		for (final FactDescriptor def : definition.getAttributes().values()) {
			if (def.getField().equals(idField)) {
				return true;
			}
		}
		return false;
	}
}