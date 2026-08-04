package com.retrocrawler.core.gear.parser;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;

import com.retrocrawler.core.gear.RatedFact;

/**
 * Interprets RetroCrawler's canonical archive path fact.
 * <p>
 * During an archive crawl the cached value stays relative to its configured
 * archive root. During located gear resolution the returned {@link Path} is
 * resolved against the root configured for that crawler. Detached resolution
 * retains a relative path.
 */
public final class PathParser implements FactParser<Path> {

	@Override
	public RatedFact<Path> parse(final String rawValue, final FactParseContext context) {
		Objects.requireNonNull(context, "context");
		if (rawValue == null || rawValue.isBlank()) {
			return RatedFact.none("Expected a non-empty archive-relative path.");
		}

		final Path relative;
		try {
			relative = Path.of(rawValue).normalize();
		} catch (final InvalidPathException failure) {
			return RatedFact.none("Invalid archive-relative path: " + rawValue);
		}
		if (relative.isAbsolute() || relative.toString().isEmpty() || relative.startsWith("..")) {
			return RatedFact.none("Expected an archive-relative path but got: " + rawValue);
		}

		final Path effective = context.archiveRoot().map(root -> root.resolve(relative).normalize()).orElse(relative);
		return RatedFact.exact(effective);
	}
}
