package com.retrocrawler.core.gear;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.parser.FactParseContext;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.util.RetroAttribute;

public class FactFinder {

	private final String key;

	private final FactParser<?> parser;

	private final Class<?> fieldType;

	private final boolean strict;

	private final boolean contextual;

	public FactFinder(final String key, final FactParser<?> parser, final Class<?> fieldType, final boolean strict) {
		this(key, parser, fieldType, strict, false);
	}

	public FactFinder(final String key, final FactParser<?> parser, final Class<?> fieldType, final boolean strict,
			final boolean contextual) {
		this.key = Objects.requireNonNull(key, "key");
		this.parser = Objects.requireNonNull(parser, "parser");
		this.fieldType = Objects.requireNonNull(fieldType, "fieldType");
		this.strict = strict;
		this.contextual = contextual;
	}

	public String key() {
		return key;
	}

	public FactParser<?> parser() {
		return parser;
	}

	public Class<?> fieldType() {
		return fieldType;
	}

	public boolean isStrict() {
		return strict;
	}

	public boolean isContextual() {
		return contextual;
	}

	public Optional<Fact> find(final Clue clue) {
		return find(clue, FactParseContext.detached());
	}

	public Optional<Fact> find(final Clue clue, final FactParseContext context) {
		Objects.requireNonNull(context, "context");
		final Set<String> raws = clue.value();
		if (raws.isEmpty()) {
			return Optional.empty();
		}

		final Set<Object> values = HashSet.newHashSet(raws.size());
		Confidence overall = Confidence.EXACT;

		Class<?> commonType = null;

		for (final String raw : raws) {
			final RatedFact<?> rated = parser.parse(raw, context);

			if (rated.confidence() == Confidence.NONE) {
				// Not successfully parsed -> no Fact at all.
				return Optional.empty();
			}

			final Object parsedValue = rated.value().orElseThrow(() -> new IllegalStateException("Parser "
					+ parser.getClass().getName() + " returned confidence " + rated.confidence() + " but no value."));
			final Class<?> parsedType = parsedValue.getClass();
			if (commonType == null) {
				commonType = parsedType;
			} else if (parsedType != commonType) {
				// Mixed runtime types are not allowed in a single Fact value set.
				return Optional.empty();
			}
			values.add(parsedValue);

			// Aggregate confidence: keep the weakest (worst) one.
			if (rated.confidence().compareTo(overall) > 0) {
				overall = rated.confidence();
			}
		}

		if (values.size() > 1 && !acceptsMultipleValues()) {
			return Optional.empty();
		}

		return Optional.of(new Fact(key, Set.copyOf(values), overall, clue));
	}

	private boolean acceptsMultipleValues() {
		return Collection.class.isAssignableFrom(fieldType) || RetroAttribute.class.isAssignableFrom(fieldType);
	}

	public RatedFact<?> parse(final String raw) {
		return parser.parse(raw);
	}

	public RatedFact<?> parse(final String raw, final FactParseContext context) {
		return parser.parse(raw, context);
	}

}
