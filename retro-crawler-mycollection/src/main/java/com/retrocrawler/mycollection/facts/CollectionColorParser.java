package com.retrocrawler.mycollection.facts;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.model.appearance.Color;
import com.retrocrawler.model.appearance.ColorParser;

/**
 * Adapts established German color markers from this collection to the portable
 * named-color vocabulary.
 */
public final class CollectionColorParser implements FactParser<Color> {

	private final ColorParser delegate = new ColorParser();

	@Override
	public RatedFact<Color> parse(final String rawValue) {
		return delegate.parse(rawValue == null ? null : canonical(rawValue));
	}

	private static String canonical(final String rawValue) {
		return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
		case "beige" -> "beige";
		case "blau" -> "blue";
		case "braun" -> "brown";
		case "gelb" -> "yellow";
		case "grau" -> "gray";
		case "grün", "gruen" -> "green";
		case "lila" -> "purple";
		case "mehrfarbig" -> "multicolored";
		case "rot" -> "red";
		case "schwarz" -> "black";
		case "silber" -> "silver";
		case "türkis", "tuerkis" -> "turquoise";
		case "violett" -> "violet";
		case "weiß", "weiss" -> "white";
		default -> rawValue;
		};
	}
}
