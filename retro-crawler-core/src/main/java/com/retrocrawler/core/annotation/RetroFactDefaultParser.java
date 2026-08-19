package com.retrocrawler.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Instant;
import java.time.LocalDate;

import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.gear.parser.ARIParser;
import com.retrocrawler.core.gear.parser.EnumFactParser;
import com.retrocrawler.core.gear.parser.EnumParser;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.InstantParser;
import com.retrocrawler.core.gear.parser.IntParser;
import com.retrocrawler.core.gear.parser.LocalDateParser;
import com.retrocrawler.core.gear.parser.StringParser;

/**
 * Selects the parser classes used for fixed automatically detected fact types
 * in one collection. Place this annotation on the type carrying
 * {@link RetroCollection}. A parser explicitly selected by
 * {@link RetroFact#parser()} is unaffected.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RetroFactDefaultParser {

	/** The default for {@link String} facts. */
	Class<? extends FactParser<String>> string() default StringParser.class;

	/** The shared default for {@code int} and {@link Integer} facts. */
	Class<? extends FactParser<Integer>> integer() default IntParser.class;

	/** The default for {@link Instant} facts. */
	Class<? extends FactParser<Instant>> instant() default InstantParser.class;

	/** The default for {@link LocalDate} facts. */
	Class<? extends FactParser<LocalDate>> localDate() default LocalDateParser.class;

	/** The default for {@link ARI} facts and collections of ARIs. */
	Class<? extends FactParser<ARI>> ari() default ARIParser.class;

	/**
	 * The default for enum-valued facts. The concrete enum type is taken from
	 * the fact field when the selected parser is constructed.
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends EnumFactParser> enumeration() default EnumParser.class;

}
