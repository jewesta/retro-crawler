package com.retrocrawler.core.archive;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A stable Archive Resource Identifier.
 * <p>
 * An ARI identifies one resource by collection, archive, and archive-relative
 * path. It deliberately contains no provider or physical root information, so
 * the same ARI can be resolved after an archive moves or changes provider.
 */
public final class ARI {

	public static final String SCHEME = "ari";

	private final String collectionId;

	private final ArchiveId archiveId;

	private final Path resourcePath;

	private final URI uri;

	private ARI(final String collectionId, final ArchiveId archiveId, final Path resourcePath) {
		this.collectionId = requireIdentifier(collectionId, "collectionId");
		this.archiveId = Objects.requireNonNull(archiveId, "archiveId");
		requireIdentifier(archiveId.value(), "archiveId");
		this.resourcePath = normalizeResourcePath(resourcePath);
		this.uri = createUri(this.collectionId, this.archiveId, this.resourcePath);
	}

	/** Creates an ARI for an archive-relative resource path. */
	public static ARI of(final String collectionId, final ArchiveId archiveId, final Path resourcePath) {
		return new ARI(collectionId, archiveId, resourcePath);
	}

	/** Parses the canonical URI representation of an ARI. */
	public static ARI parse(final String value) {
		Objects.requireNonNull(value, "value");
		try {
			return from(URI.create(value));
		} catch (final IllegalArgumentException failure) {
			throw new IllegalArgumentException("Invalid ARI: " + value, failure);
		}
	}

	/** Creates an ARI from its URI representation. */
	public static ARI from(final URI uri) {
		Objects.requireNonNull(uri, "uri");
		if (!SCHEME.equalsIgnoreCase(uri.getScheme()) || uri.isOpaque() || uri.getRawAuthority() != null
				|| uri.getRawQuery() != null || uri.getRawFragment() != null) {
			throw new IllegalArgumentException(
					"Expected a hierarchical '" + SCHEME + "' URI without authority, query, or fragment: " + uri);
		}

		final String rawPath = uri.getRawPath();
		if (rawPath == null || !rawPath.startsWith("/")) {
			throw new IllegalArgumentException(
					"Expected an ARI path containing collection and archive identity: " + uri);
		}
		final String[] rawSegments = rawPath.substring(1).split("/", -1);
		if (rawSegments.length < 2 || rawSegments[0].isEmpty() || rawSegments[1].isEmpty()) {
			throw new IllegalArgumentException(
					"Expected an ARI path containing collection and archive identity: " + uri);
		}

		final String collectionId = decode(rawSegments[0]);
		final ArchiveId archiveId = ArchiveId.of(decode(rawSegments[1]));
		final List<String> resourceSegments = new ArrayList<>();
		for (int index = 2; index < rawSegments.length; index++) {
			final String segment = decode(rawSegments[index]);
			if (segment.isEmpty()) {
				throw new IllegalArgumentException("ARI resource paths must not contain empty segments: " + uri);
			}
			if (segment.indexOf('/') >= 0 || segment.indexOf('\\') >= 0) {
				throw new IllegalArgumentException("ARI resource paths must use URI path separators: " + uri);
			}
			resourceSegments.add(segment);
		}

		try {
			final Path resourcePath = resourceSegments.isEmpty() ? Path.of("")
					: Path.of(resourceSegments.getFirst(),
							resourceSegments.subList(1, resourceSegments.size()).toArray(String[]::new));
			return new ARI(collectionId, archiveId, resourcePath);
		} catch (final InvalidPathException failure) {
			throw new IllegalArgumentException("ARI resource path cannot be represented on this platform: " + uri,
					failure);
		}
	}

	/** The collection namespace in which this resource is identified. */
	public String collectionId() {
		return collectionId;
	}

	/** The archive containing this resource. */
	public ArchiveId archiveId() {
		return archiveId;
	}

	/** The resource path relative to the archive root. */
	public Path resourcePath() {
		return resourcePath;
	}

	/** The canonical URI representation. */
	public URI uri() {
		return uri;
	}

	@Override
	public int hashCode() {
		return uri.hashCode();
	}

	@Override
	public boolean equals(final Object object) {
		return object instanceof ARI other && uri.equals(other.uri);
	}

	@Override
	public String toString() {
		return uri.toASCIIString();
	}

	private static String requireIdentifier(final String value, final String name) {
		Objects.requireNonNull(value, name);
		if (value.isBlank()) {
			throw new IllegalArgumentException(name + " must not be blank.");
		}
		return value;
	}

	private static Path normalizeResourcePath(final Path resourcePath) {
		Objects.requireNonNull(resourcePath, "resourcePath");
		if (resourcePath.isAbsolute()) {
			throw new IllegalArgumentException(
					"ARI resource path must be relative to its archive root: " + resourcePath);
		}
		if (!resourcePath.toString().isEmpty()) {
			for (final Path segment : resourcePath) {
				final String name = segment.toString();
				if (".".equals(name) || "..".equals(name)) {
					throw new IllegalArgumentException(
							"ARI resource path must not contain traversal segments: " + resourcePath);
				}
				if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0) {
					throw new IllegalArgumentException(
							"ARI resource path contains a non-portable path segment: " + resourcePath);
				}
			}
		}
		return resourcePath.normalize();
	}

	private static URI createUri(final String collectionId, final ArchiveId archiveId, final Path resourcePath) {
		final StringBuilder value = new StringBuilder(SCHEME).append(":/").append(encode(collectionId)).append('/')
				.append(encode(archiveId.value()));
		if (!resourcePath.toString().isEmpty()) {
			for (final Path segment : resourcePath) {
				value.append('/').append(encode(segment.toString()));
			}
		}
		try {
			return new URI(value.toString());
		} catch (final URISyntaxException impossible) {
			throw new IllegalArgumentException("Could not create ARI.", impossible);
		}
	}

	private static String encode(final String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
	}

	private static String decode(final String value) {
		return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
	}
}
