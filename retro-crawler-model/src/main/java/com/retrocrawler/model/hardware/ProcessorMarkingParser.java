package com.retrocrawler.model.hardware;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.ParseContext;

/**
 * Parses canonical AMD production markings and Intel FPO/partial-ATPO markings.
 */
public final class ProcessorMarkingParser implements FactParser<ProcessorMarking> {

	private static final Pattern AMD = Pattern.compile("^([A-Z][A-Z0-9]?)-(\\d{3,4})([A-Z0-9]{3,4}(?:-[A-Z])?)$");

	private static final Pattern INTEL = Pattern
			.compile("^([A-Z]\\d(?:0[1-9]|[1-4]\\d|5[0-3])[A-Z0-9]{4})(?:-(\\d{4}))?$");

	@Override
	public RatedFact<ProcessorMarking> parse(final String rawValue, final ParseContext context) {
		if (rawValue == null) {
			return noMatch();
		}

		final Matcher amd = AMD.matcher(rawValue);
		if (amd.matches()) {
			return RatedFact.exact(new AmdProcessorMarking(amd.group(1), amd.group(2), amd.group(3)));
		}

		final Matcher intel = INTEL.matcher(rawValue);
		if (intel.matches()) {
			return RatedFact.exact(new IntelProcessorMarking(intel.group(1), Optional.ofNullable(intel.group(2))));
		}

		return noMatch();
	}

	private static RatedFact<ProcessorMarking> noMatch() {
		return RatedFact.none("Expected a canonical AMD or Intel processor production marking.");
	}
}
