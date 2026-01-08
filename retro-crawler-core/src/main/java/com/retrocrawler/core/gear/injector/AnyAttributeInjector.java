package com.retrocrawler.core.gear.injector;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import com.retrocrawler.core.gear.AnyAttributeMode;
import com.retrocrawler.core.util.RetroAttribute;

final class AnyAttributeInjector implements Injector {

	@Override
	public void inject(final GearInjectionSession session) {
		session.getDescriptor().getAnyAttributeField().ifPresent(field -> inject(field, session));
	}

	private void inject(final Field anyField, final GearInjectionSession session) {
		final Map<String, RetroAttribute> payload = buildPayload(session);
		FieldSetter.setAnyAttributeMap(session.getGear(), anyField, payload);
	}

	private static Map<String, RetroAttribute> buildPayload(final GearInjectionSession session) {
		final AnyAttributeMode mode = session.getDescriptor().getAnyAttributeMode();

		return switch (mode) {
		case UNASSIGNED_ONLY -> session.getUnassigned();
		case ALL_FACTS_AND_CLUES -> session.getAttributes().getAll();
		case CLUES_ONLY -> session.getAttributes().getClues().stream().collect(LinkedHashMap::new,
				(map, clue) -> map.put(clue.getKey(), clue), Map::putAll);
		case FACTS_ONLY -> session.getAttributes().getFacts().stream().collect(LinkedHashMap::new,
				(map, fact) -> map.put(fact.getKey(), fact), Map::putAll);
		};
	}

}