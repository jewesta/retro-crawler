package com.retrocrawler.core.archive;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.StringJoiner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/** Compacts and expands clue source ARIs using their archive-tree context. */
final class ArchiveJsonSources {

	private ArchiveJsonSources() {
	}

	static void compact(final JsonNode storedArchive) {
		transform(storedArchive, true);
	}

	static void expand(final JsonNode storedArchive) {
		transform(storedArchive, false);
	}

	private static void transform(final JsonNode storedArchive, final boolean compact) {
		final ObjectNode archive = object(storedArchive, "archive");
		final String collectionId = text(archive.get("collectionId"), "archive collectionId");
		final ArchiveId archiveId = ArchiveId.of(text(archive.get("id"), "archive id"));
		transformNode(object(archive.get("root"), "archive root"), Path.of(""), collectionId, archiveId, compact);
	}

	private static void transformNode(final ObjectNode node, final Path resourcePath, final String collectionId,
			final ArchiveId archiveId, final boolean compact) {
		final JsonNode artifact = node.get("artifact");
		if (artifact != null && !artifact.isNull()) {
			transformArtifact(object(artifact, "artifact at " + resourcePath), resourcePath, collectionId, archiveId,
					compact);
		}

		final JsonNode children = node.get("children");
		if (children == null || children.isNull()) {
			return;
		}
		if (!children.isArray()) {
			throw malformed("Archive children must be an array or null at: " + resourcePath);
		}
		for (final JsonNode child : children) {
			final ObjectNode childObject = object(child, "archive child at " + resourcePath);
			final String folder = text(childObject.get("folder"), "archive child folder at " + resourcePath);
			transformNode(childObject, childPath(resourcePath, folder), collectionId, archiveId, compact);
		}
	}

	private static void transformArtifact(final ObjectNode artifact, final Path artifactPath, final String collectionId,
			final ArchiveId archiveId, final boolean compact) {
		artifact.properties().forEach(entry -> {
			final ObjectNode clue = object(entry.getValue(), "clue '" + entry.getKey() + "'");
			final JsonNode sources = clue.get("sources");
			if (sources == null) {
				return;
			}
			if (!(sources instanceof final ArrayNode array)) {
				throw malformed("Clue '" + entry.getKey() + "' sources must be an array.");
			}
			for (int index = 0; index < array.size(); index++) {
				final String source = text(array.get(index), "clue '" + entry.getKey() + "' source");
				array.set(index, TextNode.valueOf(compact ? compact(source, collectionId, archiveId, artifactPath)
						: expand(source, collectionId, archiveId, artifactPath)));
			}
		});
	}

	private static String compact(final String stored, final String collectionId, final ArchiveId archiveId,
			final Path artifactPath) {
		final ARI source = ARI.parse(stored);
		if (!collectionId.equals(source.collectionId()) || !archiveId.equals(source.archiveId())
				|| !isAtOrBelow(source.resourcePath(), artifactPath)) {
			throw malformed("Clue source '" + source + "' is outside artifact '"
					+ ARI.of(collectionId, archiveId, artifactPath) + "'.");
		}
		final Path relative = artifactPath.relativize(source.resourcePath());
		return relative.toString().isEmpty() ? "." : "./" + portable(relative);
	}

	private static String expand(final String stored, final String collectionId, final ArchiveId archiveId,
			final Path artifactPath) {
		if (".".equals(stored)) {
			return ARI.of(collectionId, archiveId, artifactPath).toString();
		}
		if (!stored.startsWith("./") || stored.length() == 2) {
			throw malformed("Expected a clue source to be '.' or an artifact-relative './...' path but got: " + stored);
		}
		return ARI.of(collectionId, archiveId, artifactPath.resolve(relativePath(stored.substring(2)))).toString();
	}

	private static Path relativePath(final String stored) {
		final String[] segments = stored.split("/", -1);
		for (final String segment : segments) {
			if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment) || segment.indexOf('\\') >= 0) {
				throw malformed("Invalid artifact-relative clue source: ./" + stored);
			}
		}
		try {
			return Path.of(segments[0], Arrays.copyOfRange(segments, 1, segments.length));
		} catch (final InvalidPathException failure) {
			throw malformed("Invalid artifact-relative clue source: ./" + stored, failure);
		}
	}

	private static Path childPath(final Path parent, final String folder) {
		try {
			final Path child = Path.of(folder);
			if (child.isAbsolute() || child.getNameCount() != 1 || ".".equals(folder) || "..".equals(folder)
					|| folder.indexOf('/') >= 0 || folder.indexOf('\\') >= 0) {
				throw malformed("Archive child folder must be one relative path segment: " + folder);
			}
			return parent.resolve(child);
		} catch (final InvalidPathException failure) {
			throw malformed("Invalid archive child folder: " + folder, failure);
		}
	}

	private static boolean isAtOrBelow(final Path resourcePath, final Path artifactPath) {
		return artifactPath.toString().isEmpty() || resourcePath.equals(artifactPath)
				|| resourcePath.startsWith(artifactPath);
	}

	private static String portable(final Path path) {
		final StringJoiner result = new StringJoiner("/");
		path.forEach(segment -> result.add(segment.toString()));
		return result.toString();
	}

	private static ObjectNode object(final JsonNode value, final String description) {
		if (!(value instanceof final ObjectNode object)) {
			throw malformed("Expected " + description + " to be an object.");
		}
		return object;
	}

	private static String text(final JsonNode value, final String description) {
		if (value == null || !value.isTextual()) {
			throw malformed("Expected " + description + " to be a string.");
		}
		return value.textValue();
	}

	private static IllegalArgumentException malformed(final String message) {
		return new IllegalArgumentException(message);
	}

	private static IllegalArgumentException malformed(final String message, final RuntimeException failure) {
		return new IllegalArgumentException(message, failure);
	}
}
