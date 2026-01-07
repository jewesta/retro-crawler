package com.retrocrawler.core.gear;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.parser.FactParser;

public class FactFinder {

	private final String key;

	private final FactParser parser;

	private final Class<?> fieldType;

	private final boolean strict;

	public FactFinder(final String key, final FactParser parser, final Class<?> fieldType, final boolean strict) {
		this.key = Objects.requireNonNull(key, "key");
		this.parser = Objects.requireNonNull(parser, "parser");
		this.fieldType = Objects.requireNonNull(fieldType, "fieldType");
		this.strict = strict;
	}

	public String getKey() {
		return key;
	}

	public FactParser getParser() {
		return parser;
	}

	public Class<?> getFieldType() {
		return fieldType;
	}

	public boolean isStrict() {
		return strict;
	}

	public Optional<Fact> find(final Clue clue) {
		final Set<String> raws = clue.getValue();

		final Set<Object> values = HashSet.newHashSet(raws.size());
		Confidence overall = Confidence.EXACT;

		Class<?> commonType = null;

		for (final String raw : raws) {
			final RatedFact rated = parser.parse(raw);

			if (rated.getConfidence() == Confidence.NONE) {
				// Not successfully parsed -> no Fact at all.
				return Optional.empty();
			}

			final Object parsed = rated.getValue()
					.orElseThrow(() -> new IllegalStateException("Parser " + parser.getClass().getSimpleName()
							+ " returned confidence " + rated.getConfidence() + " but no value."));

			final Class<?> parsedType = parsed.getClass();
			if (commonType == null) {
				commonType = parsedType;
			} else if (parsedType != commonType) {
				// Mixed runtime types are not allowed in a single Fact value set.
				return Optional.empty();
			}

			values.add(parsed);

			// Aggregate confidence: keep the weakest (worst) one.
			if (rated.getConfidence().compareTo(overall) > 0) {
				overall = rated.getConfidence();
			}
		}

		return Optional.of(new Fact(key, Set.copyOf(values), overall, clue));
	}

	public RatedFact parse(final String raw) {
		return parser.parse(raw);
	}

}