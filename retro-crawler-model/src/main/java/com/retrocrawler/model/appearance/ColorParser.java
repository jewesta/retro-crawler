package com.retrocrawler.model.appearance;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

/**
 * Parses portable English names for visible colors. Collection-specific
 * languages and compound spellings belong in collection adapters.
 */
public final class ColorParser implements FactParser {

	@Override
	public RatedFact parse(final String rawValue) {
		if (rawValue == null) {
			return noMatch();
		}

		final Color direct = parseSingle(rawValue);
		if (direct != null) {
			return RatedFact.exact(direct);
		}

		final String[] components = rawValue.trim().split("\\s*[/\\-]\\s*", -1);
		if (components.length < 2) {
			return noMatch();
		}
		final Set<Color> colors = new LinkedHashSet<>();
		for (final String component : components) {
			final Color color = parseSingle(component);
			if (color == null) {
				return noMatch();
			}
			colors.add(color);
		}
		return colors.size() == 1 ? RatedFact.exact(colors.iterator().next()) : RatedFact.exact(Set.copyOf(colors));
	}

	private static Color parseSingle(final String rawValue) {
		final String normalized = rawValue.trim().toUpperCase(Locale.ROOT)
				.replaceAll("[\\s_-]+", "");
		return switch (normalized) {
		case "BEIGE" -> Color.BEIGE;
		case "BLACK" -> Color.BLACK;
		case "BLUE" -> Color.BLUE;
		case "BROWN" -> Color.BROWN;
		case "CYAN" -> Color.CYAN;
		case "GOLD" -> Color.GOLD;
		case "GRAY", "GREY" -> Color.GRAY;
		case "GREEN" -> Color.GREEN;
		case "MAGENTA" -> Color.MAGENTA;
		case "MULTICOLOR", "MULTICOLOUR", "MULTICOLORED", "MULTICOLOURED" -> Color.MULTICOLORED;
		case "ORANGE" -> Color.ORANGE;
		case "PINK" -> Color.PINK;
		case "PURPLE" -> Color.PURPLE;
		case "RED" -> Color.RED;
		case "SILVER" -> Color.SILVER;
		case "TURQUOISE" -> Color.TURQUOISE;
		case "VIOLET" -> Color.VIOLET;
		case "WHITE" -> Color.WHITE;
		case "YELLOW" -> Color.YELLOW;
		default -> null;
		};
	}

	private static RatedFact noMatch() {
		return RatedFact.none("Expected a recognized named color.");
	}
}
