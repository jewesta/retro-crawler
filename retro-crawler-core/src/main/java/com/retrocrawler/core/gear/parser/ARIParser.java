package com.retrocrawler.core.gear.parser;

import java.nio.file.Path;
import java.util.Objects;

import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.gear.RatedFact;

/** Interprets an artifact-relative resource path as an {@link ARI}. */
public final class ARIParser implements FactParser<ARI> {

	@Override
	public RatedFact<ARI> parse(final String rawValue, final ParseContext context) {
		Objects.requireNonNull(context, "context");
		if (rawValue == null || rawValue.isBlank()) {
			return RatedFact.none("Expected a non-empty artifact-relative resource path.");
		}

		try {
			return RatedFact.exact(context.source().resolve(Path.of(rawValue)));
		} catch (final IllegalArgumentException failure) {
			return RatedFact.none("Invalid artifact-relative resource path: " + rawValue);
		}
	}
}
