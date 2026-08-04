package com.retrocrawler.model.hardware;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParseContext;
import com.retrocrawler.core.gear.parser.FactParser;

public final class MemoryAccessTimeParser implements FactParser<MemoryAccessTime> {

	private static final Pattern TIME = Pattern.compile("(?:NS\\s*(\\d+)|(\\d+)\\s*NS)");

	@Override
	public RatedFact<MemoryAccessTime> parse(final String rawValue, final FactParseContext context) {
		if (rawValue == null) {
			return RatedFact.none("Expected a positive memory access time in nanoseconds.");
		}

		final Matcher matcher = TIME.matcher(rawValue.trim().toUpperCase(Locale.ROOT));
		if (!matcher.matches()) {
			return RatedFact.none("Expected a positive memory access time in nanoseconds.");
		}

		try {
			final String nanoseconds = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
			return RatedFact.exact(new MemoryAccessTime(Integer.parseInt(nanoseconds)));
		} catch (final IllegalArgumentException e) {
			return RatedFact.none("Expected a positive memory access time in nanoseconds.");
		}
	}
}
