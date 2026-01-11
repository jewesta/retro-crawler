package com.retrocrawler.demo.collection.clues;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.PathNameClueFinder;
import com.retrocrawler.demo.collection.AttributeNames;

public class SquareBracketsClueFinder implements PathNameClueFinder {

	public static final String OPENING_BRACKETS = "[";

	public static final String CLOSING_BRACKETS = "]";

	private static final Clue parse(final String keyValueString) {
		final String trimmed = keyValueString.trim();

		final int firstSpace = trimmed.indexOf(' ');

		final String key;
		final String valuesPart;

		if (firstSpace > 0) {
			final String possibleKey = trimmed.substring(0, firstSpace).trim();

			// A key must not contain a comma
			if (!possibleKey.contains(",")) {
				key = possibleKey;
				valuesPart = trimmed.substring(firstSpace + 1).trim();
			} else {
				key = null;
				valuesPart = trimmed;
			}
		} else {
			key = null;
			valuesPart = trimmed;
		}

		if (valuesPart.isEmpty()) {
			throw new IllegalArgumentException("Expected non-empty value part but found empty string.");
		}

		final String[] rawValues = valuesPart.split(",\\s+");

		final Set<String> values = new HashSet<>();
		for (final String raw : rawValues) {
			if (!raw.isEmpty()) {
				values.add(raw);
			}
		}

		if (values.isEmpty()) {
			throw new IllegalArgumentException("Expected at least one value but found none.");
		}

		if (key == null) {
			return Clue.of(values);
		}
		return Clue.of(key, values);
	}

	@Override
	public Set<Clue> find(String folderName) {
		if (!folderName.contains("[")) {
			return Set.of();
		}

		final Set<Clue> clues = new HashSet<>();

		folderName = folderName.trim();
		if (folderName.isEmpty()) {
			// Nothing to see here
			return clues;
		}

		/*
		 * See if there is a title. By convention, everything before the first attribute
		 * in [] is the title.
		 */
		final int indexOfFirstAttribute = folderName.indexOf(SquareBracketsClueFinder.OPENING_BRACKETS);
		if (indexOfFirstAttribute == -1) {
			// Path name has only title attribute
			clues.add(Clue.of(AttributeNames.TITLE, folderName));
			return clues;
		}

		if (indexOfFirstAttribute > 0) {
			final String title = folderName.substring(0, indexOfFirstAttribute).trim();
			if (!title.isEmpty()) {
				clues.add(Clue.of(AttributeNames.TITLE, title));
			}
		}

		final String attributeString = folderName.substring(indexOfFirstAttribute, folderName.length());
		final Matcher m = Pattern.compile("\\" + SquareBracketsClueFinder.OPENING_BRACKETS + "(.*?)\\"
				+ SquareBracketsClueFinder.CLOSING_BRACKETS).matcher(attributeString);
		while (m.find()) {
			final String keyValueString = m.group(1).trim();
			if (keyValueString.isEmpty()) {
				// Empty attribute tag - ignore
				continue;
			}
			final Clue clue = parse(keyValueString);
			clues.add(clue);
		}
		return clues;
	}

}
