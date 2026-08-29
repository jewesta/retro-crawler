package com.retrocrawler.core.gear.parser;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * A parser for one concrete enum type. Implementations may be selected as the
 * default for automatically detected enum facts. Unless a builder factory is
 * registered, such an implementation must have a public constructor accepting
 * the concrete enum {@link Class}.
 *
 * @param <T>
 *            the concrete enum type
 */
public interface EnumFactParser<T extends Enum<T>> extends ChoiceFactParser<T> {

	/**
	 * The concrete enum domain. Direct implementations inherit reflective type
	 * discovery; generic parser implementations should override this method.
	 */
	@SuppressWarnings("unchecked")
	default Class<T> enumType() {
		for (final Type contract : getClass().getGenericInterfaces()) {
			if (contract instanceof final ParameterizedType parameterized
					&& parameterized.getRawType() == EnumFactParser.class
					&& parameterized.getActualTypeArguments()[0] instanceof final Class<?> type && type.isEnum()) {
				return (Class<T>) type;
			}
		}
		throw new IllegalStateException("Cannot infer the enum type of " + getClass().getName()
				+ ". Generic EnumFactParser implementations must override enumType().");
	}

	@Override
	default List<T> choices() {
		return List.of(enumType().getEnumConstants());
	}

}
