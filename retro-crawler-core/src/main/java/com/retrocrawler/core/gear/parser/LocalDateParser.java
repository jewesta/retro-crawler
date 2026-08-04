package com.retrocrawler.core.gear.parser;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.time.LocalDate;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.retrocrawler.core.gear.RatedFact;

/**
 * Parses complete calendar dates using ISO syntax and the configured locale.
 */
public final class LocalDateParser implements FactParser<LocalDate> {

	private static final List<FormatStyle> LOCALIZED_STYLES = List.of(FormatStyle.FULL, FormatStyle.LONG,
			FormatStyle.MEDIUM, FormatStyle.SHORT);
	private static final List<Character> NUMERIC_SEPARATORS = List.of('.', '-', '/');
	private static final ConcurrentMap<Locale, List<DateTimeFormatter>> FORMATTERS = new ConcurrentHashMap<>();

	@Override
	public RatedFact<LocalDate> parse(final String rawValue, final ParseContext context) {
		Objects.requireNonNull(context, "context");
		if (rawValue == null || rawValue.isBlank()) {
			return RatedFact.none("Expected a complete calendar date.");
		}

		final String value = rawValue.trim();
		final Locale locale = context.config().locale();
		final Set<LocalDate> candidates = new LinkedHashSet<>();
		for (final DateTimeFormatter formatter : FORMATTERS.computeIfAbsent(locale, LocalDateParser::formatters)) {
			try {
				candidates.add(LocalDate.parse(value, formatter));
			} catch (final DateTimeParseException ignored) {
				// Another configured representation may still match.
			}
		}

		if (candidates.isEmpty()) {
			return RatedFact.none(
					"Expected a complete calendar date for locale " + locale.toLanguageTag() + " but got: " + rawValue);
		}
		if (candidates.size() > 1) {
			return RatedFact.none("Ambiguous calendar date for locale " + locale.toLanguageTag() + ": " + rawValue);
		}
		return RatedFact.exact(candidates.iterator().next());
	}

	private static List<DateTimeFormatter> formatters(final Locale locale) {
		final List<DateTimeFormatter> result = new ArrayList<>();
		result.add(DateTimeFormatter.ISO_LOCAL_DATE);

		for (final FormatStyle style : LOCALIZED_STYLES) {
			final String localizedPattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(style, null,
					IsoChronology.INSTANCE, locale);
			final String strictPattern = useFourDigitProlepticYear(localizedPattern);
			result.add(new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(strictPattern)
					.toFormatter(locale).withResolverStyle(ResolverStyle.STRICT));
		}

		final String shortPattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT, null,
				IsoChronology.INSTANCE, locale);
		final List<ChronoField> fieldOrder = numericFieldOrder(shortPattern);
		for (final char separator : NUMERIC_SEPARATORS) {
			result.add(numericFormatter(fieldOrder, separator, locale));
		}
		return List.copyOf(result);
	}

	private static DateTimeFormatter numericFormatter(final List<ChronoField> fieldOrder, final char separator,
			final Locale locale) {
		final DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
		for (int index = 0; index < fieldOrder.size(); index++) {
			if (index > 0) {
				builder.appendLiteral(separator);
			}
			final ChronoField field = fieldOrder.get(index);
			if (field == YEAR) {
				builder.appendValue(field, 4);
			} else {
				builder.appendValue(field, 1, 2, SignStyle.NOT_NEGATIVE);
			}
		}
		return builder.toFormatter(locale).withResolverStyle(ResolverStyle.STRICT);
	}

	private static List<ChronoField> numericFieldOrder(final String pattern) {
		final List<ChronoField> order = new ArrayList<>(3);
		boolean quoted = false;
		for (int index = 0; index < pattern.length(); index++) {
			final char token = pattern.charAt(index);
			if (token == '\'') {
				if (index + 1 < pattern.length() && pattern.charAt(index + 1) == '\'') {
					index++;
				} else {
					quoted = !quoted;
				}
				continue;
			}
			if (quoted) {
				continue;
			}

			final ChronoField field = switch (token) {
			case 'd' -> DAY_OF_MONTH;
			case 'M', 'L' -> MONTH_OF_YEAR;
			case 'u', 'y' -> YEAR;
			default -> null;
			};
			if (field != null && !order.contains(field)) {
				order.add(field);
			}
		}
		return order.size() == 3 ? List.copyOf(order) : List.of(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH);
	}

	private static String useFourDigitProlepticYear(final String pattern) {
		final StringBuilder result = new StringBuilder(pattern.length() + 2);
		boolean quoted = false;
		for (int index = 0; index < pattern.length(); index++) {
			final char token = pattern.charAt(index);
			if (token == '\'') {
				result.append(token);
				if (index + 1 < pattern.length() && pattern.charAt(index + 1) == '\'') {
					result.append(pattern.charAt(++index));
				} else {
					quoted = !quoted;
				}
				continue;
			}
			if (!quoted && token == 'y') {
				while (index + 1 < pattern.length() && pattern.charAt(index + 1) == 'y') {
					index++;
				}
				result.append("uuuu");
			} else {
				result.append(token);
			}
		}
		return result.toString();
	}
}
