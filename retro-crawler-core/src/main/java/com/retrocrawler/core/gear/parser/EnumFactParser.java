package com.retrocrawler.core.gear.parser;

/**
 * A parser for one concrete enum type. Implementations may be selected as the
 * default for automatically detected enum facts. Unless a builder factory is
 * registered, such an implementation must have a public constructor accepting
 * the concrete enum {@link Class}.
 *
 * @param <T>
 *            the concrete enum type
 */
public interface EnumFactParser<T extends Enum<T>> extends FactParser<T> {

}
