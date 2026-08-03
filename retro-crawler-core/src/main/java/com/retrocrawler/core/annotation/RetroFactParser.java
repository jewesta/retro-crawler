package com.retrocrawler.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.retrocrawler.core.gear.parser.FactParser;

/**
 * Overrides collection-specific configuration for a fact parser selected by a
 * field-level {@link RetroFact}. This annotation does not itself select or
 * instantiate the parser.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(RetroFactParser.Container.class)
public @interface RetroFactParser {

	Class<? extends FactParser> parser();

	/**
	 * Catalog file relative to the collection working directory's
	 * {@code catalogs} folder.
	 */
	String catalogFile() default "";

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface Container {

		RetroFactParser[] value();
	}
}
