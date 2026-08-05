package com.retrocrawler.mycollection.clues;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.ClueAccumulator;
import com.retrocrawler.core.archive.clues.ClueFileIOException;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.archive.clues.FileContentClueFinder;
import com.retrocrawler.mycollection.AttributeNames;

/**
 * Reads a UTF-8 {@code retro.md} collection note. Optional flat front matter
 * supplies keyed clues while the Markdown body supplies {@code desc}.
 */
public final class RetroMarkdownClueFinder implements FileContentClueFinder {

	private static final String FILE_NAME = "retro.md";

	private static final String DELIMITER = "---";

	@Override
	public boolean matches(final String fileName) {
		return FILE_NAME.equalsIgnoreCase(fileName);
	}

	@Override
	public Clues find(final InputStream is) {
		try {
			return parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
		} catch (final IOException e) {
			throw new ClueFileIOException("Could not read " + FILE_NAME + ".", e);
		}
	}

	private static Clues parse(final String document) {
		final Line first = lineAt(document, 0);
		if (!DELIMITER.equals(first.text())) {
			return document.isBlank() ? Clues.none() : Clues.of(Clue.of(AttributeNames.DESC, document));
		}

		final Map<String, Set<String>> valuesByKey = new LinkedHashMap<>();
		int cursor = first.next();
		while (cursor < document.length()) {
			final Line line = lineAt(document, cursor);
			if (DELIMITER.equals(line.text())) {
				return clues(valuesByKey, document.substring(line.next()));
			}
			parseFrontMatterLine(line.text(), valuesByKey);
			cursor = line.next();
		}
		throw new ClueFileIOException("Unclosed front matter in " + FILE_NAME + ".");
	}

	private static void parseFrontMatterLine(final String rawLine, final Map<String, Set<String>> valuesByKey) {
		final String line = rawLine.trim();
		if (line.isEmpty() || line.startsWith("#")) {
			return;
		}

		final int colon = line.indexOf(':');
		if (colon <= 0) {
			throw new ClueFileIOException("Expected flat 'key: value' front matter but got: " + rawLine);
		}

		final String key = line.substring(0, colon).trim();
		if (AttributeNames.DESC.equalsIgnoreCase(key)) {
			throw new ClueFileIOException("Put desc in the Markdown body, not in front matter.");
		}

		final String value = line.substring(colon + 1).trim();
		final Set<String> values = valuesByKey.computeIfAbsent(key, ignored -> new LinkedHashSet<>());
		if (AttributeNames.LOT.equalsIgnoreCase(key)) {
			java.util.Arrays.stream(value.split(",\\s+", -1)).map(String::trim).forEach(values::add);
		} else {
			values.add(value);
		}
	}

	private static Clues clues(final Map<String, Set<String>> valuesByKey, final String rawBody) {
		final ClueAccumulator clues = Clues.accumulator();
		for (final Map.Entry<String, Set<String>> entry : valuesByKey.entrySet()) {
			final Set<String> nonEmptyValues = entry.getValue().stream().filter(value -> !value.isEmpty())
					.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
			clues.add(nonEmptyValues.isEmpty() ? Clue.missingValue(entry.getKey())
					: Clue.of(entry.getKey(), nonEmptyValues));
		}

		final String body = rawBody.strip();
		if (!body.isEmpty()) {
			clues.add(Clue.of(AttributeNames.DESC, body));
		}
		return clues.clues();
	}

	private static Line lineAt(final String document, final int offset) {
		final int lineFeed = document.indexOf('\n', offset);
		if (lineFeed < 0) {
			final int end = document.endsWith("\r") ? document.length() - 1 : document.length();
			return new Line(document.substring(offset, end), document.length());
		}
		final int end = lineFeed > offset && document.charAt(lineFeed - 1) == '\r' ? lineFeed - 1 : lineFeed;
		return new Line(document.substring(offset, end), lineFeed + 1);
	}

	private record Line(String text, int next) {
	}
}
