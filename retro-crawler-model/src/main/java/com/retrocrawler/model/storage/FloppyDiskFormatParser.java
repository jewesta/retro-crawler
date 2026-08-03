package com.retrocrawler.model.storage;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.model.storage.FloppyDiskFormat.Density;
import com.retrocrawler.model.storage.FloppyDiskFormat.Sides;

public final class FloppyDiskFormatParser implements FactParser<FloppyDiskFormat> {

	private static final Pattern NUMERIC_COMBINED = Pattern.compile("^([12])S([124])D$");
	private static final Pattern NUMERIC_AND_NAMED = Pattern.compile("^([12])S(SD|DD|QD|HD|ED)$");
	private static final Pattern NAMED_COMBINED = Pattern.compile("^(SS|DS)(SD|DD|QD|HD|ED)$");

	@Override
	public RatedFact<FloppyDiskFormat> parse(final String rawValue) {
		if (rawValue == null) {
			return noMatch();
		}

		final String normalized = rawValue.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s_\\-/]+", "");
		final FloppyDiskFormat standalone = standalone(normalized);
		if (standalone != null) {
			return RatedFact.exact(standalone);
		}

		final Matcher numeric = NUMERIC_COMBINED.matcher(normalized);
		if (numeric.matches()) {
			return RatedFact.exact(FloppyDiskFormat.of(sides(numeric.group(1)), density(numeric.group(2))));
		}

		final Matcher numericAndNamed = NUMERIC_AND_NAMED.matcher(normalized);
		if (numericAndNamed.matches()) {
			return RatedFact
					.exact(FloppyDiskFormat.of(sides(numericAndNamed.group(1)), density(numericAndNamed.group(2))));
		}

		final Matcher named = NAMED_COMBINED.matcher(normalized);
		if (named.matches()) {
			return RatedFact.exact(FloppyDiskFormat.of(sides(named.group(1)), density(named.group(2))));
		}

		return noMatch();
	}

	private static FloppyDiskFormat standalone(final String normalized) {
		return switch (normalized) {
		case "SS" -> FloppyDiskFormat.sides(Sides.SINGLE);
		case "DS" -> FloppyDiskFormat.sides(Sides.DOUBLE);
		case "SD" -> FloppyDiskFormat.density(Density.SINGLE);
		case "DD" -> FloppyDiskFormat.density(Density.DOUBLE);
		case "QD" -> FloppyDiskFormat.density(Density.QUAD);
		case "HD" -> FloppyDiskFormat.density(Density.HIGH);
		case "ED" -> FloppyDiskFormat.density(Density.EXTENDED);
		default -> null;
		};
	}

	private static Sides sides(final String value) {
		return switch (value) {
		case "1", "SS" -> Sides.SINGLE;
		case "2", "DS" -> Sides.DOUBLE;
		default -> throw new IllegalArgumentException("Unsupported floppy-disk side count: " + value);
		};
	}

	private static Density density(final String value) {
		return switch (value) {
		case "1", "SD" -> Density.SINGLE;
		case "2", "DD" -> Density.DOUBLE;
		case "4", "QD" -> Density.QUAD;
		case "HD" -> Density.HIGH;
		case "ED" -> Density.EXTENDED;
		default -> throw new IllegalArgumentException("Unsupported floppy-disk density: " + value);
		};
	}

	private static RatedFact<FloppyDiskFormat> noMatch() {
		return RatedFact.none("Expected SS/DS sides, SD/DD/QD/HD/ED density, or a conventional combination.");
	}
}
