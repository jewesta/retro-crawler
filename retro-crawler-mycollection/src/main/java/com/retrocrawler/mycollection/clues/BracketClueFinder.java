package com.retrocrawler.mycollection.clues;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.retrocrawler.core.archive.clues.ArchiveFolderView;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.ClueAccumulator;
import com.retrocrawler.core.archive.clues.ClueFinder;
import com.retrocrawler.core.archive.clues.ClueLocation;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.mycollection.AttributeNames;

/**
 * Reads the collection's bracket language while retaining unknown or malformed
 * groups as anonymous clues.
 */
public final class BracketClueFinder implements ClueFinder {

	@Override
	public Clues find(final ArchiveFolderView folder) {
		final String folderName = folder.name();
		Objects.requireNonNull(folderName, "folderName");

		final int firstOpeningBracket = folderName.indexOf('[');
		if (firstOpeningBracket < 0) {
			return Clues.none();
		}

		final ClueAccumulator clues = Clues.accumulator();
		final List<String> titleParts = new java.util.ArrayList<>();
		int cursor = 0;

		while (cursor < folderName.length()) {
			final int openingBracket = folderName.indexOf('[', cursor);
			if (openingBracket < 0) {
				addTitlePart(titleParts, folderName.substring(cursor));
				break;
			}

			addTitlePart(titleParts, folderName.substring(cursor, openingBracket));

			final int closingBracket = folderName.indexOf(']', openingBracket + 1);
			if (closingBracket < 0) {
				clues.add(Clue.of(folderName.substring(openingBracket).trim()),
						ClueLocation.in(folderName, openingBracket, folderName.length() - openingBracket));
				cursor = folderName.length();
				break;
			}

			/*
			 * The whole group including its brackets is the observation, so a
			 * rejected duplicate points at the tag the cataloguer wrote rather
			 * than at the normalized key it produced.
			 */
			final String group = folderName.substring(openingBracket + 1, closingBracket);
			final ClueLocation location = ClueLocation.in(folderName, openingBracket,
					closingBracket - openingBracket + 1);
			parseGroup(group).ifPresent(clue -> clues.add(clue, location));
			cursor = closingBracket + 1;
		}

		final String title = String.join(" ", titleParts);
		if (!clues.isEmpty() && !title.isBlank()) {
			clues.add(Clue.of(AttributeNames.TITLE, title), ClueLocation.in(folderName, 0, folderName.length()));
		}

		return clues.clues();
	}

	private static void addTitlePart(final List<String> titleParts, final String raw) {
		final String normalized = raw.trim().replaceAll("\\s+", " ");
		if (!normalized.isEmpty()) {
			titleParts.add(normalized);
		}
	}

	private static Optional<Clue> parseGroup(final String rawGroup) {
		final String group = rawGroup.trim();
		if (group.isEmpty()) {
			return Optional.empty();
		}

		final int firstWhitespace = firstWhitespace(group);
		if (firstWhitespace < 0) {
			return Optional.of(Clue.of(splitValues(group)));
		}

		final String possibleKey = group.substring(0, firstWhitespace);
		final String rawValues = group.substring(firstWhitespace).trim();
		if (possibleKey.contains(",") || rawValues.isEmpty()) {
			return Optional.of(Clue.of(splitValues(group)));
		}

		try {
			return Optional.of(Clue.of(possibleKey.toLowerCase(Locale.ROOT), splitValues(rawValues)));
		} catch (final IllegalArgumentException e) {
			/*
			 * Reserved or otherwise invalid keys are still valuable
			 * observations. Keep the complete group as an anonymous clue rather
			 * than losing it.
			 */
			return Optional.of(Clue.of(group));
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
		return Arrays.stream(rawValues.split(",\\s+", -1)).map(String::trim)
				.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
	}
}
