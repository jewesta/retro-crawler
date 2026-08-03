package com.retrocrawler.model.measurement;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

/**
 * Parses uniform capacity sets in either count-first or capacity-first order.
 */
public final class CapacitySetParser implements FactParser {

	private static final Pattern COUNT_FIRST = Pattern.compile("^(\\d+)\\s*[x×]\\s*(.+)$", Pattern.CASE_INSENSITIVE);

	private static final Pattern CAPACITY_FIRST = Pattern.compile("^(.+?)\\s*[x×]\\s*(\\d+)$",
			Pattern.CASE_INSENSITIVE);

	@Override
	public RatedFact parse(final String rawValue) {
		return parseValue(rawValue).<RatedFact> map(RatedFact::exact).orElseGet(
				() -> RatedFact.none("Expected '<member count> x <capacity per member>' or the reverse order."));
	}

	public static Optional<CapacitySet> parseValue(final String rawValue) {
		if (rawValue == null) {
			return Optional.empty();
		}

		final String normalized = rawValue.trim();
		final Matcher countFirst = COUNT_FIRST.matcher(normalized);
		if (countFirst.matches()) {
			return capacitySet(countFirst.group(1), countFirst.group(2));
		}

		final Matcher capacityFirst = CAPACITY_FIRST.matcher(normalized);
		if (capacityFirst.matches()) {
			return capacitySet(capacityFirst.group(2), capacityFirst.group(1));
		}
		return Optional.empty();
	}

	private static Optional<CapacitySet> capacitySet(final String rawMemberCount, final String rawCapacity) {
		try {
			final int memberCount = Integer.parseInt(rawMemberCount);
			return DataCapacityParser.parseValue(rawCapacity).map(capacity -> new CapacitySet(memberCount, capacity));
		} catch (final IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
