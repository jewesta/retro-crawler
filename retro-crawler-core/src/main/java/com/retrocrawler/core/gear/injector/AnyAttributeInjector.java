package com.retrocrawler.core.gear.injector;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import com.retrocrawler.core.gear.AnyAttributeMode;
import com.retrocrawler.core.util.RetroAttribute;

final class AnyAttributeInjector implements Injector {

	@Override
	public void inject(final GearInjectionSession session) {
		session.descriptor().getAnyAttributeField().ifPresent(field -> inject(field, session));
	}

	private void inject(final Field anyField, final GearInjectionSession session) {
		final Map<String, RetroAttribute> payload = buildPayload(session);
		FieldSetter.setAnyAttributeMap(session.gear(), anyField, payload);
	}

	private static Map<String, RetroAttribute> buildPayload(final GearInjectionSession session) {
		final AnyAttributeMode mode = session.descriptor().getAnyAttributeMode();

		return switch (mode) {
		case UNASSIGNED_ONLY -> new LinkedHashMap<>(session.unassigned());
		case ALL_FACTS_AND_CLUES -> {
			final Map<String, RetroAttribute> out = new LinkedHashMap<>();
			session.allCluesByKey().values().forEach(clue -> out.put(clue.getKey(), clue));
			session.allFactsByKey().values().forEach(fact -> out.put(fact.getKey(), fact));
			yield out;
		}
		case CLUES_ONLY -> session.allCluesByKey().values().stream().collect(LinkedHashMap::new,
				(map, clue) -> map.put(clue.getKey(), clue), Map::putAll);
		case FACTS_ONLY -> session.allFactsByKey().values().stream().collect(LinkedHashMap::new,
				(map, fact) -> map.put(fact.getKey(), fact), Map::putAll);
		};
	}

}