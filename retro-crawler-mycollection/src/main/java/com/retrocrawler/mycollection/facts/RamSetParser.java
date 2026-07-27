package com.retrocrawler.mycollection.facts;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.model.measurement.DataCapacity;
import com.retrocrawler.model.measurement.DataCapacityParser;
import com.retrocrawler.mycollection.memory.RamSet;

public final class RamSetParser implements FactParser {

	private static final Pattern SET = Pattern.compile("^(\\d+)\\s*x\\s*(.+)$", Pattern.CASE_INSENSITIVE);

	@Override
	public RatedFact parse(final String rawValue) {
		if (rawValue == null) {
			return RatedFact.none("Expected '<member count> x <capacity per member>'.");
		}

		final Matcher matcher = SET.matcher(rawValue.trim());
		if (!matcher.matches()) {
			return RatedFact.none("Expected '<member count> x <capacity per member>'.");
		}

		try {
			final int memberCount = Integer.parseInt(matcher.group(1));
			final Optional<DataCapacity> capacity = DataCapacityParser.parseValue(matcher.group(2));
			if (capacity.isEmpty()) {
				return RatedFact.none("Expected a supported capacity after the RAM-set member count.");
			}
			return RatedFact.exact(new RamSet(memberCount, capacity.get()));
		} catch (final IllegalArgumentException e) {
			return RatedFact.none("Expected at least two RAM-set members and a supported per-member capacity.");
		}
	}
}
