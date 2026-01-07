package com.retrocrawler.core.gear.parser;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import com.retrocrawler.core.gear.RatedFact;

public class EnumParser<T extends Enum<T>> implements FactParser {

	private final Class<T> enumType;

	private final BiPredicate<T, String> matcher;

	public EnumParser(final Class<T> enumType) {
		this(enumType, true);
	}

	public EnumParser(final Class<T> enumType, final BiPredicate<T, String> matcher) {
		this.enumType = Objects.requireNonNull(enumType);
		this.matcher = Objects.requireNonNull(matcher);
	}

	public EnumParser(final Class<T> enumType, final boolean ignoreCase) {
		this.enumType = Objects.requireNonNull(enumType);
		if (ignoreCase) {
			this.matcher = EnumParser::matchIgnoreCase;
		} else {
			this.matcher = EnumParser::matchExactly;
		}
	}

	private static <T extends Enum<T>> boolean matchIgnoreCase(final T type, final String value) {
		return type.name().equalsIgnoreCase(value);
	}

	private static <T extends Enum<T>> boolean matchExactly(final T type, final String value) {
		return type.name().equals(value);
	}

	@Override
	public RatedFact parse(final String rawValue) {
		for (final T constant : enumType.getEnumConstants()) {
			if (matcher.test(constant, rawValue)) {
				return RatedFact.exact(constant);
			}
		}
		return RatedFact.none("Could not construct an " + enumType.getSimpleName() + " for value " + rawValue
				+ ": There is no corresponding enum constant. Expected one of: "
				+ Arrays.stream(enumType.getEnumConstants()).map(T::name).map(String::toLowerCase)
						.collect(Collectors.joining(", "))
				+ ".");
	}

}
