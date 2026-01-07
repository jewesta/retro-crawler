package com.retrocrawler.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.retrocrawler.core.gear.Fact;
import com.retrocrawler.core.gear.parser.AutoDetectParser;
import com.retrocrawler.core.gear.parser.FactParser;

/**
 * Injects a {@link Fact} into a field. The field must be one of the following:
 * <ul>
 * <li>Fact / RetroAttribute: Injects the fact object directly.</li>
 * <li>Set<T>: Injects the values of the fact. Will also work with a
 * Collection<T></li>. The types must be compatible.
 * <li>T: Injects the single fact value. Will throw if the fact has more than
 * one values.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RetroFact {

	/**
	 * Can be used to rename the key that is used to get a value for the annotated
	 * field. Default key is name of field.
	 */
	String key() default "";

	/**
	 * Set this to true to prevent null values. In this case retro crawler will
	 * throw if there is no value for the field (or if it is null).
	 */
	boolean optional() default true;

	/**
	 * In strict mode a value is only assigned if the key matches a clue. In lenient
	 * mode retro crawler might assign the value of an anonymous clue if the data
	 * types match.
	 */
	boolean strict() default true;

	Class<? extends FactParser> parser() default AutoDetectParser.class;

}
