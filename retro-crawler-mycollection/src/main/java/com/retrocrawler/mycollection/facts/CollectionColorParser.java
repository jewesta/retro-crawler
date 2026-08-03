package com.retrocrawler.mycollection.facts;

import java.util.Arrays;
import java.util.Locale;

import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.model.appearance.ColorParser;

/**
 * Adapts established German and compound color markers from this collection
 * to the portable named-color vocabulary.
 */
public final class CollectionColorParser implements FactParser {

	private final ColorParser delegate = new ColorParser();

	@Override
	public RatedFact parse(final String rawValue) {
		if (rawValue == null) {
			return delegate.parse(null);
		}

		final String canonical = canonical(rawValue);
		final RatedFact direct = delegate.parse(canonical);
		if (direct.confidence() != Confidence.NONE) {
			return direct;
		}

		final String[] components = rawValue.trim().split("\\s*[/\\-]\\s*", -1);
		if (components.length < 2) {
			return direct;
		}
		return delegate.parse(Arrays.stream(components).map(CollectionColorParser::canonical)
				.reduce((left, right) -> left + "/" + right).orElse(rawValue));
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
