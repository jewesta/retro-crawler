package com.retrocrawler.core.gear.parser;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Runtime archive location available while a raw clue is interpreted.
 * <p>
 * Cached clues remain independent of a particular deployment. Parsers that
 * interpret archive-relative paths can use this context to bind those paths to
 * the roots configured for the current crawl.
 */
public record FactParseContext(Optional<Path> archiveRoot, Optional<Path> artifactPath) {

	private static final FactParseContext DETACHED = new FactParseContext(Optional.empty(), Optional.empty());

	public FactParseContext {
		archiveRoot = Objects.requireNonNull(archiveRoot, "archiveRoot");
		artifactPath = Objects.requireNonNull(artifactPath, "artifactPath");
		if (archiveRoot.isPresent() != artifactPath.isPresent()) {
			throw new IllegalArgumentException("Archive root and artifact path must either both be present or absent.");
		}
		if (archiveRoot.isPresent()
				&& !artifactPath.orElseThrow().normalize().startsWith(archiveRoot.orElseThrow().normalize())) {
			throw new IllegalArgumentException("Artifact path must be below its archive root.");
		}
	}

	public static FactParseContext detached() {
		return DETACHED;
	}

	public static FactParseContext located(final Path archiveRoot, final Path artifactPath) {
		Objects.requireNonNull(archiveRoot, "archiveRoot");
		Objects.requireNonNull(artifactPath, "artifactPath");
		return new FactParseContext(Optional.of(archiveRoot), Optional.of(artifactPath));
	}
}
