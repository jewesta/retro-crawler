package com.retrocrawler.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Path;

import com.retrocrawler.core.gear.parser.EnumParser;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.IntParser;
import com.retrocrawler.core.gear.parser.PathParser;
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

	/** The default for {@link Path} facts and collections of paths. */
	Class<? extends FactParser<Path>> path() default PathParser.class;

}
