package com.retrocrawler.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.retrocrawler.core.archive.clues.Clue;

/**
 * Injects a {@link Clue} into a field. The field must be one of the following:
 * <ul>
 * <li>Clue / RetroAttribute: Injects the clue object directly.</li>
 * <li>Set<String>: Injects the values of the clue. Will also work with a
 * Collection<String></li>
 * <li>String: Injects the single clue value. Will throw if the clue has more
 * than one values.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RetroClue {

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

}
