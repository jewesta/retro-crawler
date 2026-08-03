package com.retrocrawler.model.locale;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.FactParser;

public final class LanguageCodeParser implements FactParser {

	private static final Map<String, String> ENGLISH_NAMES = Arrays.stream(Locale.getISOLanguages()).collect(Collectors
			.toUnmodifiableMap(LanguageCodeParser::englishName, Function.identity(), (first, ignored) -> first));

	@Override
	public RatedFact parse(final String rawValue) {
		if (rawValue == null) {
			return noMatch();
		}

		final String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
		final String code = LanguageCode.isSupported(normalized) ? normalized : ENGLISH_NAMES.get(normalized);
		if (code == null) {
			return noMatch();
		}
		return RatedFact.exact(new LanguageCode(code));
	}

	private static String englishName(final String code) {
		return Locale.forLanguageTag(code).getDisplayLanguage(Locale.ENGLISH).toLowerCase(Locale.ROOT);
	}

	private static RatedFact noMatch() {
		return RatedFact.none("Expected an ISO 639-1 language code or English language name.");
	}
}
