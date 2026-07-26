package com.retrocrawler.mycollection.clues;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.PathNameClueFinder;
import com.retrocrawler.mycollection.AttributeNames;

/**
 * Reads the collection's bracket language while retaining unknown or malformed
 * groups as anonymous clues.
 */
public final class BracketPathClueFinder implements PathNameClueFinder {

	@Override
	public Set<Clue> find(final String pathName) {
		Objects.requireNonNull(pathName, "pathName");

		final int firstOpeningBracket = pathName.indexOf('[');
		if (firstOpeningBracket < 0) {
			return Set.of();
		}

		final Set<Clue> clues = new LinkedHashSet<>();
		final List<String> titleParts = new java.util.ArrayList<>();
		int cursor = 0;

		while (cursor < pathName.length()) {
			final int openingBracket = pathName.indexOf('[', cursor);
			if (openingBracket < 0) {
				addTitlePart(titleParts, pathName.substring(cursor));
				break;
			}

			addTitlePart(titleParts, pathName.substring(cursor, openingBracket));

			final int closingBracket = pathName.indexOf(']', openingBracket + 1);
			if (closingBracket < 0) {
				clues.add(Clue.of(pathName.substring(openingBracket).trim()));
				cursor = pathName.length();
				break;
			}

			final String group = pathName.substring(openingBracket + 1, closingBracket);
			clues.add(parseGroup(group));
			cursor = closingBracket + 1;
		}

		final String title = String.join(" ", titleParts);
		if (!title.isBlank()) {
			clues.add(Clue.of(AttributeNames.TITLE, title));
		}

		return Set.copyOf(clues);
	}

	private static void addTitlePart(final List<String> titleParts, final String raw) {
		final String normalized = raw.trim().replaceAll("\\s+", " ");
		if (!normalized.isEmpty()) {
			titleParts.add(normalized);
		}
	}

	private static Clue parseGroup(final String rawGroup) {
		final String group = rawGroup.trim();
		if (group.isEmpty()) {
			return Clue.of("");
		}

		final int firstWhitespace = firstWhitespace(group);
		if (firstWhitespace < 0) {
			return Clue.of(splitValues(group));
		}

		final String possibleKey = group.substring(0, firstWhitespace);
		final String rawValues = group.substring(firstWhitespace).trim();
		if (possibleKey.contains(",") || rawValues.isEmpty()) {
			return Clue.of(group);
		}

		try {
			return Clue.of(possibleKey, splitValues(rawValues));
		} catch (final IllegalArgumentException e) {
			/*
			 * Reserved or otherwise invalid keys are still valuable observations. Keep
			 * the complete group as an anonymous clue rather than losing it.
			 */
			return Clue.of(group);
		}
	}

	private static int firstWhitespace(final String value) {
		for (int i = 0; i < value.length(); i++) {
			if (Character.isWhitespace(value.charAt(i))) {
				return i;
			}
		}
		return -1;
	}

	private static Set<String> splitValues(final String rawValues) {
		return Arrays.stream(rawValues.split(",", -1)).map(String::trim)
				.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
	}
}
