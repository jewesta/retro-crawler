package com.retrocrawler.model.appearance;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.EnumFactParser;
import com.retrocrawler.core.gear.parser.ParseContext;

/**
 * Parses portable English names for visible colors. Collection-specific
 * languages belong in collection adapters.
 */
public final class ColorParser implements EnumFactParser<Color> {

	@Override
	public Class<Color> enumType() {
		return Color.class;
	}

	@Override
	public RatedFact<Color> parse(final String rawValue, final ParseContext context) {
		if (rawValue == null) {
			return noMatch();
		}

		final Color color = parseSingle(rawValue);
		return color == null ? noMatch() : RatedFact.exact(color);
	}

	private static Color parseSingle(final String rawValue) {
		final String normalized = rawValue.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s_-]+", "");
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

	private static RatedFact<Color> noMatch() {
		return RatedFact.none("Expected a recognized named color.");
	}
}
