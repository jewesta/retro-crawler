package com.retrocrawler.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the optional fallback Gear type of a model.
 * <p>
 * The fallback is produced when no {@link RetroGear} matcher recognizes an
 * artifact or when several equally confident matches cannot be resolved by the
 * Gear type hierarchy. A model may declare at most one fallback Gear.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RetroAnyGear {

	/**
	 * Stable model key for this Gear type. The lower-case, hyphen-separated
	 * simple class name is used when omitted. The resulting key must be unique
	 * among all Gear types in the model.
	 */
	String key() default "";

	/**
	 * Human-facing name for this Gear type. A lower-case name derived from the
	 * simple class name is used when omitted.
	 */
	String name() default "";

}
