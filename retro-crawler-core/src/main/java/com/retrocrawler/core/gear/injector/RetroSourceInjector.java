package com.retrocrawler.core.gear.injector;

import java.lang.reflect.Field;

final class RetroSourceInjector implements Injector {

	@Override
	public void inject(final GearInjectionSession session) {
		session.descriptor().sourceField().ifPresent(field -> inject(field, session));
	}

	private static void inject(final Field sourceField, final GearInjectionSession session) {
		FieldSetter.setField(session.gear(), sourceField, session.source());
	}
}
