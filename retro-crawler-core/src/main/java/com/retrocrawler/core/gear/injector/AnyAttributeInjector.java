package com.retrocrawler.core.gear.injector;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import com.retrocrawler.core.gear.AnyAttributeMode;
import com.retrocrawler.core.util.RetroAttribute;

final class AnyAttributeInjector implements Injector {

	@Override
	public void inject(final GearInjectionSession session) {
		session.descriptor().anyAttributeField().ifPresent(field -> inject(field, session));
	}

	private void inject(final Field anyField, final GearInjectionSession session) {
		final Map<String, RetroAttribute> payload = buildPayload(session);
		FieldSetter.setAnyAttributeMap(session.gear(), anyField, payload);
	}

	private static Map<String, RetroAttribute> buildPayload(final GearInjectionSession session) {
		final AnyAttributeMode mode = session.descriptor().anyAttributeMode();

		return switch (mode) {
		case UNASSIGNED_ONLY -> session.unassigned();
		case ALL_FACTS_AND_CLUES -> session.attributes().all();
		case CLUES_ONLY -> session.attributes().clues().stream().collect(LinkedHashMap::new,
				(map, clue) -> map.put(clue.key(), clue), Map::putAll);
		case FACTS_ONLY -> session.attributes().facts().stream().collect(LinkedHashMap::new,
				(map, fact) -> map.put(fact.key(), fact), Map::putAll);
		};
	}

}