package com.retrocrawler.model.appearance;

import java.util.Optional;

/**
 * A named visible color rather than a measured RGB or spectral value.
 */
public enum Color {

	BEIGE("#F5F5DC"),
	BLACK("#000000"),
	BLUE("#0000FF"),
	BROWN("#A52A2A"),
	CYAN("#00FFFF"),
	GOLD("#FFD700"),
	GRAY("#808080"),
	GREEN("#008000"),
	MAGENTA("#FF00FF"),
	MULTICOLORED(null),
	ORANGE("#FFA500"),
	PINK("#FFC0CB"),
	PURPLE("#800080"),
	RED("#FF0000"),
	SILVER("#C0C0C0"),
	TURQUOISE("#40E0D0"),
	VIOLET("#EE82EE"),
	WHITE("#FFFFFF"),
	YELLOW("#FFFF00");

	private final String colorCode;

	Color(final String colorCode) {
		this.colorCode = colorCode;
	}

	/**
	 * A CSS-compatible sRGB hexadecimal value when this named color has one.
	 */
	public Optional<String> getColorCode() {
		return Optional.ofNullable(colorCode);
	}
}
