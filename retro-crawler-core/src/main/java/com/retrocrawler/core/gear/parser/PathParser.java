package com.retrocrawler.core.gear.parser;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;

import com.retrocrawler.core.gear.RatedFact;

/**
 * Interprets RetroCrawler's canonical archive resource path fact.
 * <p>
 * During an archive crawl the cached value stays relative to its artifact.
 * During located gear resolution the returned {@link Path} is resolved against
 * the source path of that artifact.
 */
public final class PathParser implements FactParser<Path> {

	@Override
	public RatedFact<Path> parse(final String rawValue, final ParseContext context) {
		Objects.requireNonNull(context, "context");
		if (rawValue == null || rawValue.isBlank()) {
			return RatedFact.none("Expected a non-empty artifact-relative path.");
		}

		final Path relative;
		try {
			relative = Path.of(rawValue).normalize();
		} catch (final InvalidPathException failure) {
			return RatedFact.none("Invalid artifact-relative path: " + rawValue);
		}
		if (relative.isAbsolute() || relative.toString().isEmpty() || relative.startsWith("..")) {
			return RatedFact.none("Expected an artifact-relative path but got: " + rawValue);
		}

		final Path effective = context.currentNode().path().resolve(relative).normalize();
		return RatedFact.exact(effective);
	}
}
